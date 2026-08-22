import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'
import * as google from 'googleapis'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const REPORTING_SCOPE = 'https://www.googleapis.com/auth/playdeveloperreporting'
const DEFAULT_HOURS = 24
const VALID_REPORT_TYPES = ['CRASH', 'ANR', 'NON_FATAL'] as const

type ReportType = typeof VALID_REPORT_TYPES[number]
type ErrorReport = google.playdeveloperreporting_v1beta1.Schema$GooglePlayDeveloperReportingV1beta1ErrorReport
type ReportingApi = google.playdeveloperreporting_v1beta1.Playdeveloperreporting
type SearchParams = google.playdeveloperreporting_v1beta1.Params$Resource$Vitals$Errors$Reports$Search

interface Settings {
    applicationId: string
}

interface Options {
    hours: number
    reportTypes: ReportType[]
    versionCode?: number
    output?: string
    help: boolean
}

interface SavedReport {
    applicationId: string
    interval: {
        start: string
        end: string
    }
    filter: string
    fetchedAt: string
    reports: ErrorReport[]
}

const usage = `Google Play のクラッシュ・ANR レポートを取得する。

Usage:
  npm run fetch-crashes -- [options]

Options:
  --hours <number>         取得する完了済み時間数（既定: 24）
  --type <types>           CRASH, ANR, NON_FATAL のカンマ区切り（既定: CRASH）
  --version-code <number>  versionCode で絞り込む
  --output <path>          結果を JSON ファイルにも保存する
  --help                   このヘルプを表示する
`

const requireValue = (args: string[], index: number, option: string): string => {
    const value = args[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`${option} requires a value.`)
    return value
}

const parsePositiveInteger = (value: string, option: string): number => {
    if (!/^[1-9]\d*$/.test(value)) throw new Error(`${option} must be a positive integer.`)
    const parsed = Number(value)
    if (!Number.isSafeInteger(parsed)) throw new Error(`${option} is too large.`)
    return parsed
}

const parseReportTypes = (value: string): ReportType[] => {
    const values = value.split(',').map(item => item.trim().toUpperCase()).filter(Boolean)
    if (values.length === 0) throw new Error('--type requires at least one report type.')

    const invalid = values.filter(value => !VALID_REPORT_TYPES.includes(value as ReportType))
    if (invalid.length > 0) {
        throw new Error(`Unknown report type: ${invalid.join(', ')}.`)
    }

    return [...new Set(values as ReportType[])]
}

const parseArguments = (args: string[]): Options => {
    const options: Options = {
        hours: DEFAULT_HOURS,
        reportTypes: ['CRASH'],
        help: false,
    }

    for (let index = 0; index < args.length; index++) {
        const argument = args[index]
        switch (argument) {
            case '--hours':
                options.hours = parsePositiveInteger(requireValue(args, index, argument), argument)
                index++
                break
            case '--type':
                options.reportTypes = parseReportTypes(requireValue(args, index, argument))
                index++
                break
            case '--version-code':
                options.versionCode = parsePositiveInteger(requireValue(args, index, argument), argument)
                index++
                break
            case '--output':
                options.output = requireValue(args, index, argument)
                index++
                break
            case '--help':
                options.help = true
                break
            default:
                throw new Error(`Unknown option: ${argument}.`)
        }
    }

    return options
}

const loadSettings = (): Settings => {
    const settingsFile = path.resolve(__dirname, '../settings.json')
    const settings = JSON.parse(fs.readFileSync(settingsFile, 'utf8')) as Partial<Settings>
    if (!settings.applicationId || typeof settings.applicationId !== 'string') {
        throw new Error(`applicationId is missing in ${settingsFile}.`)
    }
    return { applicationId: settings.applicationId }
}

const getCompletedInterval = (hours: number): { start: Date, end: Date } => {
    const end = new Date()
    end.setUTCMinutes(0, 0, 0)
    const start = new Date(end.getTime() - hours * 60 * 60 * 1000)
    return { start, end }
}

const buildFilter = (options: Options): string => {
    const typeFilter = options.reportTypes
        .map(type => `errorIssueType = ${type}`)
        .join(' OR ')
    const filters = options.reportTypes.length > 1 ? [`(${typeFilter})`] : [typeFilter]
    if (options.versionCode !== undefined) filters.push(`versionCode = ${options.versionCode}`)
    return filters.join(' AND ')
}

const addDateTime = (
    params: SearchParams,
    prefix: 'interval.startTime' | 'interval.endTime',
    value: Date,
): void => {
    const target = params as SearchParams & Record<string, string | number>
    target[`${prefix}.year`] = value.getUTCFullYear()
    target[`${prefix}.month`] = value.getUTCMonth() + 1
    target[`${prefix}.day`] = value.getUTCDate()
    target[`${prefix}.hours`] = value.getUTCHours()
    target[`${prefix}.timeZone.id`] = 'UTC'
}

const findAppResource = async (api: ReportingApi, applicationId: string): Promise<string> => {
    let pageToken: string | undefined
    do {
        const response = await api.apps.search({
            pageSize: 1000,
            pageToken,
        })
        const app = response.data.apps?.find(candidate => candidate.packageName === applicationId)
        if (app?.name) return app.name
        pageToken = response.data.nextPageToken ?? undefined
    } while (pageToken)

    throw new Error(
        `Application ${applicationId} is not accessible. Check the Play Console read-only permission.`,
    )
}

const fetchReports = async (
    api: ReportingApi,
    parent: string,
    filter: string,
    interval: { start: Date, end: Date },
): Promise<ErrorReport[]> => {
    const reports: ErrorReport[] = []
    let pageToken: string | undefined

    do {
        const params: SearchParams = {
            parent,
            filter,
            pageSize: 100,
            pageToken,
        }
        addDateTime(params, 'interval.startTime', interval.start)
        addDateTime(params, 'interval.endTime', interval.end)

        const response = await api.vitals.errors.reports.search(params)
        reports.push(...response.data.errorReports ?? [])
        pageToken = response.data.nextPageToken ?? undefined
    } while (pageToken)

    return reports.sort((left, right) =>
        (right.eventTime ?? '').localeCompare(left.eventTime ?? ''),
    )
}

const formatDevice = (report: ErrorReport): string => {
    const device = report.deviceModel
    const id = [device?.deviceId?.buildBrand, device?.deviceId?.buildDevice]
        .filter(Boolean)
        .join('/')
    if (device?.marketingName && id) return `${device.marketingName} (${id})`
    return (device?.marketingName ?? id) || '-'
}

const printReports = (
    reports: ErrorReport[],
    settings: Settings,
    filter: string,
    interval: { start: Date, end: Date },
): void => {
    console.log(`Application: ${settings.applicationId}`)
    console.log(`Interval:    ${interval.start.toISOString()} .. ${interval.end.toISOString()}`)
    console.log(`Filter:      ${filter}`)
    console.log(`Reports:     ${reports.length}`)

    reports.forEach((report, index) => {
        console.log('')
        console.log(`[${index + 1}] ${report.type ?? 'UNKNOWN'}  ${report.eventTime ?? '-'}`)
        console.log(`Name:        ${report.name ?? '-'}`)
        console.log(`Version:     ${report.appVersion?.versionCode ?? '-'}`)
        console.log(`Android API: ${report.osVersion?.apiLevel ?? '-'}`)
        console.log(`Device:      ${formatDevice(report)}`)
        console.log(`Issue:       ${report.issue ?? '-'}`)
        if (report.vcsInformation) console.log(`VCS:         ${report.vcsInformation}`)
        console.log('')
        console.log('----- reportText -----')
        console.log(report.reportText ?? '')
    })
}

const saveReports = (
    output: string,
    reports: ErrorReport[],
    settings: Settings,
    filter: string,
    interval: { start: Date, end: Date },
): string => {
    const outputFile = path.resolve(process.cwd(), output)
    if (fs.existsSync(outputFile)) {
        throw new Error(`Refusing to overwrite existing file: ${outputFile}`)
    }

    fs.mkdirSync(path.dirname(outputFile), { recursive: true })
    const result: SavedReport = {
        applicationId: settings.applicationId,
        interval: {
            start: interval.start.toISOString(),
            end: interval.end.toISOString(),
        },
        filter,
        fetchedAt: new Date().toISOString(),
        reports,
    }
    fs.writeFileSync(outputFile, `${JSON.stringify(result, undefined, 2)}\n`, {
        encoding: 'utf8',
        mode: 0o600,
    })
    return outputFile
}

const runner = async (): Promise<void> => {
    const options = parseArguments(process.argv.slice(2))
    if (options.help) {
        console.log(usage)
        return
    }

    const settings = loadSettings()
    const interval = getCompletedInterval(options.hours)
    const filter = buildFilter(options)
    const auth = new google.Auth.GoogleAuth({ scopes: [REPORTING_SCOPE] })
    const api = new google.playdeveloperreporting_v1beta1.Playdeveloperreporting({ auth })
    const parent = await findAppResource(api, settings.applicationId)
    const reports = await fetchReports(api, parent, filter, interval)

    printReports(reports, settings, filter, interval)
    if (options.output) {
        console.log('')
        console.log(`Saved: ${saveReports(options.output, reports, settings, filter, interval)}`)
    }
}

runner().catch(error => {
    const message = error instanceof Error ? error.message : String(error)
    console.error(`Error: ${message}`)
    process.exit(1)
})
