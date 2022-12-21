import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'
import sharp from 'sharp'
import * as google from 'googleapis'
import { getCredentials, getSettings } from './utility.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const sourceDir = path.join(__dirname, '../screenshots/source')
const distDir = path.join(__dirname, '../screenshots/dist')

console.log(process.argv)

const isUpload = process.argv[2] === 'upload'

async function build() {
    fs.rmSync(distDir, { recursive: true })

    const langs = fs.readdirSync(sourceDir)
    for (const lang of langs) {
        console.log(`lang: ${lang}`)

        const sourceLangDir = path.join(sourceDir, lang)
        const distLangDir = path.join(distDir, lang)
        fs.mkdirSync(distLangDir, { recursive: true })

        const svgFileNames =
            fs.readdirSync(sourceLangDir)
                .filter(v => v.endsWith('.svg'))

        for (const svgFileName of svgFileNames) {
            const svgFile = path.join(sourceLangDir, svgFileName)
            const distSvgFile = path.join(distLangDir, path.parse(svgFileName).name + '.png')
            console.log({ svgFile, distSvgFile })

            await sharp(svgFile)
                .toFile(distSvgFile)
        }
    }
}

async function upload() {
    const settings = getSettings()
    const credentials = getCredentials()

    // create publisher
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

    const images = await publisher.edits.images.list({
        packageName: settings.applicationId,
        editId: appEditId,
        language: 'en-US',
        imageType: 'phoneScreenshots',
    })
    console.log('images', images.data.images)

    const list = await publisher.edits.listings.list({
        packageName: settings.applicationId,
        editId: appEditId,
    })
    console.log('details', list.data.listings)

    if (list.data.listings) {
        for (const listing of list.data.listings) {
            const storeFile = path.join(__dirname, '../store', listing.language!)
            fs.writeFileSync(storeFile, JSON.stringify(listing, undefined, 4))
        }
    }
    
    // const langs = fs.readdirSync(sourceDir)
    // for (const lang of langs) {
    //     console.log(`lang: ${lang}`)

    //     const sourceLangDir = path.join(sourceDir, lang)
    //     const distLangDir = path.join(distDir, lang)
    //     fs.mkdirSync(distLangDir, { recursive: true })

    //     const svgFileNames =
    //         fs.readdirSync(sourceLangDir)
    //             .filter(v => v.endsWith('.svg'))

    //     for (const svgFileName of svgFileNames) {
    //         const svgFile = path.join(sourceLangDir, svgFileName)
    //         const distSvgFile = path.join(distLangDir, path.parse(svgFileName).name + '.png')
    //         console.log({ svgFile, distSvgFile })

    //         await sharp(svgFile)
    //             .toFile(distSvgFile)
    //     }
    // }
}

await build()

if (isUpload) {
    await upload()
}