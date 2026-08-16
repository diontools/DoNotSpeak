#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <emulator-serial>" >&2
  exit 2
fi

test_serial="$1"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "$script_dir/../.." && pwd)"
android_root="$repository_root/src"
adb_command="${ADB_COMMAND:-adb}"
gradle_command="${GRADLE_COMMAND:-$android_root/gradlew}"
target_package="io.github.diontools.donotspeak.debug"
runner_component="io.github.diontools.donotspeak.debug.test/androidx.test.runner.AndroidJUnitRunner"
bluetooth_permission="android.permission.BLUETOOTH_CONNECT"
report_dir="$android_root/build/reports/bluetooth-permission-tests"

mkdir -p "$report_dir"

run_adb() {
  "$adb_command" -s "$test_serial" "$@"
}

cleanup() {
  run_adb shell am force-stop "$target_package" >/dev/null 2>&1 || true
  run_adb shell pm revoke "$target_package" "$bluetooth_permission" >/dev/null 2>&1 || true
}

capture_logcat() {
  local name="$1"
  run_adb logcat -d > "$report_dir/$name-logcat.txt" 2>&1 || true
}

prepare_permission() {
  local state="$1"

  run_adb shell am force-stop "$target_package"
  run_adb shell pm clear "$target_package" >/dev/null
  run_adb shell am force-stop "$target_package"

  if [[ "$state" == "granted" ]]; then
    run_adb shell pm grant "$target_package" "$bluetooth_permission"
  else
    run_adb shell pm revoke "$target_package" "$bluetooth_permission" >/dev/null 2>&1 || true
  fi
}

run_instrumentation_test() {
  local name="$1"
  local state="$2"
  local selector="$3"
  local output
  local result

  prepare_permission "$state"
  run_adb logcat -c

  set +e
  output="$(run_adb shell am instrument -w -r -e class "$selector" "$runner_component" 2>&1)"
  result=$?
  set -e

  printf '%s\n' "$output" | tee "$report_dir/$name-instrumentation.txt"
  capture_logcat "$name"

  if [[ $result -ne 0
        || "$output" == *"FAILURES!!!"*
        || "$output" == *"INSTRUMENTATION_FAILED"*
        || "$output" != *"OK (1 test)"* ]]; then
    echo "Instrumentation test failed: $name" >&2
    return 1
  fi
}

trap cleanup EXIT

run_adb wait-for-device
device_api="$(run_adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$device_api" != "31" && "$device_api" != "36" ]]; then
  echo "Expected an API 31 or API 36 emulator, got API $device_api" >&2
  exit 2
fi

if [[ "${DNS_SKIP_BUILD:-0}" != "1" ]]; then
  ANDROID_SERIAL="$test_serial" "$gradle_command" \
    :app:installDebug \
    :app:installDebugAndroidTest
fi

test_package="io.github.diontools.donotspeak"
run_instrumentation_test \
  "permission-denied" \
  "denied" \
  "$test_package.BluetoothPermissionServiceTest#permissionDenied_doesNotWaitForProfiles"
run_instrumentation_test \
  "setting-disabled" \
  "denied" \
  "$test_package.BluetoothPermissionServiceTest#settingDisabled_logsSettingState"
run_instrumentation_test \
  "dialog-canceled" \
  "denied" \
  "$test_package.BluetoothPermissionFlowTest#canceledDialog_preservesSettingAndStartsService"
run_instrumentation_test \
  "permission-granted" \
  "granted" \
  "$test_package.BluetoothPermissionServiceTest#permissionGranted_doesNotLogDenial"

echo "Bluetooth permission emulator tests passed on API $device_api ($test_serial)"
