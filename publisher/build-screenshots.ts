import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'
import sharp from 'sharp'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const sourceDir = path.join(__dirname, '../screenshots/source')
const distDir = path.join(__dirname, '../screenshots/dist')

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
