import * as core from '@actions/core'
import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'
import * as childProcess from 'child_process'
import fetch from 'node-fetch'
import * as crypto from 'crypto'
import * as google from 'googleapis'

console.log('publishing started')

const __dirname = path.dirname(fileURLToPath(import.meta.url))

interface Settings {
    applicationId: string
    track: string
    releaseFilePath: string
    releaseNotesDir: string
    versionCodeFile: string
}

const runner = async () => {
    // load settings
    const settingsFile = path.resolve(__dirname, 'settings.json')
    const settingsJson = fs.readFileSync(settingsFile).toString()
    const settings = JSON.parse(settingsJson) as Settings
    console.log('settings', settings)

    // load credentials
    const credentialsFile = path.resolve(__dirname, 'credentials.json')
    if (!fs.existsSync(credentialsFile)) throw `credentialsFile '${credentialsFile}' not found.`
    const credentialsJson = fs.readFileSync(credentialsFile).toString()
    const credentials = JSON.parse(credentialsJson)
    console.log('credentials.type', credentials.type)

    // resoleve aab file
    const releaseFilePath = path.resolve(__dirname, settings.releaseFilePath)
    if (!fs.existsSync(releaseFilePath)) throw `releaseFilePath '${releaseFilePath}' not found.`
    const releaseFileHash = crypto.createHash('sha256').update(fs.readFileSync(releaseFilePath)).digest('hex')
    console.log(releaseFilePath, releaseFileHash)

    // read all release notes
    const releaseNotesDir = path.resolve(__dirname, settings.releaseNotesDir)
    if (!fs.existsSync(releaseNotesDir)) throw `releaseNotesDir '${releaseNotesDir}' not found.`
    const releaseNotes =
        fs.readdirSync(releaseNotesDir)
            .flatMap(fileName => {
                const match = /^(\w+-\w+)\.txt$/i.exec(fileName)
                if (!match) return []
                const file = path.resolve(releaseNotesDir, fileName)
                const lang = match[1]
                const text = fs.readFileSync(file).toString()
                return {
                    language: lang,
                    text: text,
                } as google.androidpublisher_v3.Schema$LocalizedText
            });
    releaseNotes.forEach(note => console.log('releaseNote', note.language))

    // download bundletool
    console.log('download bundletool')
    const bundleToolResponse = await fetch("https://github.com/google/bundletool/releases/download/1.5.0/bundletool-all-1.5.0.jar")
    if (!bundleToolResponse.ok) throw `bundletool download failed. status: ${bundleToolResponse.statusText}`
    const bundleToolBinary = await bundleToolResponse.arrayBuffer()
    const bundleToolPath = path.join(__dirname, 'bundletool.jar')
    fs.writeFileSync(bundleToolPath, Buffer.from(bundleToolBinary))

    // get version code / version name
    const currentVersionCode = parseInt(childProcess.execSync(`java -jar "${bundleToolPath}" dump manifest --bundle "${releaseFilePath}" --xpath /manifest/@android:versionCode`).toString())
    const versionName = childProcess.execSync(`java -jar "${bundleToolPath}" dump manifest --bundle "${releaseFilePath}" --xpath /manifest/@android:versionName`).toString().trim()
    const releaseName = `vc${currentVersionCode} (${versionName})`
    console.log('currentVersionCode', currentVersionCode)
    console.log('versionName', versionName)
    console.log('releaseName', releaseName)

    // create android publisher
    const auth = new google.Auth.GoogleAuth({
        credentials: credentials,
        scopes: 'https://www.googleapis.com/auth/androidpublisher'
    })
    const authClient = await auth.getClient()
    const publisher = new google.androidpublisher_v3.Androidpublisher({
        auth: authClient,
    })

    // start edit
    console.log('start edit')
    const appEdit = await publisher.edits.insert({
        packageName: settings.applicationId,
    })
    const appEditId = appEdit.data.id!
    console.log('appEditId', appEditId)

    // validate track
    console.log('validate track')
    const trackList = (await publisher.edits.tracks.list({
        editId: appEditId,
        packageName: settings.applicationId,
    })).data.tracks
    trackList?.forEach(t => console.log(JSON.stringify(t, undefined, 4)))
    const targetTrack = trackList?.find(track => track.track === settings.track)
    if (!targetTrack) {
        throw 'unknown track: ' + settings.track
    }

    // get uploaded file
    console.log('get uploaded bundle list')
    const bundleList = (await publisher.edits.bundles.list({
        editId: appEditId,
        packageName: settings.applicationId,
    })).data
    const uploadedBundle = bundleList.bundles?.find(bundle => bundle.versionCode === currentVersionCode)
    console.log('uploadedBundle', uploadedBundle)

    if (uploadedBundle) {
        // file hash check
        if (uploadedBundle.sha256 !== releaseFileHash) {
            throw `file hash change detected. uploadedFileHash: ${uploadedBundle.sha256}, releaseFileHash: ${releaseFileHash}`
        }

        // patch track
        const status = targetTrack.releases?.[0].status || 'draft'
        console.log('patch track', `status: ${status}`)
        const patchedTrack = (await publisher.edits.tracks.patch({
            editId: appEditId,
            packageName: settings.applicationId,
            track: settings.track,
            requestBody: {
                track: settings.track,
                releases: [
                    {
                        name: releaseName,
                        status: status,
                        versionCodes: [currentVersionCode.toString()],
                        releaseNotes: releaseNotes,
                    }
                ]
            }
        })).data
    } else {
        // upload aab file
        console.log('upload aab file')
        const bundle = (await publisher.edits.bundles.upload({
            packageName: settings.applicationId,
            editId: appEditId,
            media: {
                mimeType: 'application/octet-stream',
                body: fs.createReadStream(releaseFilePath),
            }
        })).data

        // version code check
        const versionCode = bundle.versionCode!
        console.log('versionCode', versionCode)
        if (versionCode !== currentVersionCode) {
            throw `invalid versioncode ${versionCode} !== ${currentVersionCode}`
        }

        // update track
        console.log('update track')
        const updatedTrack = (await publisher.edits.tracks.update({
            editId: appEditId,
            packageName: settings.applicationId,
            track: settings.track,
            requestBody: {
                track: settings.track,
                releases: [
                    {
                        name: releaseName,
                        status: 'draft',
                        versionCodes: [currentVersionCode.toString()],
                        releaseNotes: releaseNotes,
                    }
                ]
            }
        })).data
    }

    // commit
    console.log('commit')
    const committed = await publisher.edits.commit({
        editId: appEditId,
        packageName: settings.applicationId,
    })

    if (!committed.data.id) throw `commit failed: ${committed.statusText}`

    console.log('committed', committed.data.id)
}

runner()
    .catch(error => core.setFailed(error))
