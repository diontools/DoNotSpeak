import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'

export interface Settings {
    applicationId: string
    track: string
    releaseFilePath: string
    releaseNotesDir: string
    versionCodeFile: string
}

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const getSettings = () => {
    const settingsFile = path.resolve(__dirname, 'settings.json')
    const settingsJson = fs.readFileSync(settingsFile).toString()
    const settings = JSON.parse(settingsJson) as Settings
    console.log('settings', settings)
    return settings
}

export const getCredentials = () => {
    const credentialsFile = path.resolve(__dirname, 'credentials.json')
    if (!fs.existsSync(credentialsFile)) throw `credentialsFile '${credentialsFile}' not found.`
    const credentialsJson = fs.readFileSync(credentialsFile).toString()
    const credentials = JSON.parse(credentialsJson)
    console.log('credentials.type', credentials.type)
    return credentials
}
