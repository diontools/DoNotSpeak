import * as core from '@actions/core'
import * as fs from 'fs'
import * as path from 'path'
import * as google from 'googleapis'

console.log('publishing started')

interface Settings {
    applicationId: string
    track: string
    releaseFilePath: string
    releaseNotesDir: string
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

    // create android publisher
    const auth = new google.Auth.GoogleAuth({
        credentials: credentials,
        scopes: 'https://www.googleapis.com/auth/androidpublisher'
    })
    const authClient = await auth.getClient()
    const publisher = new google.androidpublisher_v3.Androidpublisher({
        auth: authClient,
    })

    // resoleve aab file
    const releaseFilePath = path.resolve(__dirname, settings.releaseFilePath)
    if (!fs.existsSync(releaseFilePath)) throw `releaseFilePath '${releaseFilePath}' not found.`

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
    if (!trackList || !trackList.find(track => track.track === settings.track)) {
        throw 'unknown track: ' + settings.track
    }

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

    // get version code
    const versionCode = bundle.versionCode!
    console.log('versionCode', versionCode)

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
                    status: 'draft',
                    versionCodes: [versionCode.toString()],
                    releaseNotes: releaseNotes,
                }
            ]
        }
    })).data

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
