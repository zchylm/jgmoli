import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

func loadImage(_ path: String) -> CGImage {
    let url = URL(fileURLWithPath: path) as CFURL
    guard let source = CGImageSourceCreateWithURL(url, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        fatalError("Unable to load image: \(path)")
    }
    return image
}

func coverRect(for image: CGImage, in target: CGRect, zoom: CGFloat) -> CGRect {
    let sourceRatio = CGFloat(image.width) / CGFloat(image.height)
    let targetRatio = target.width / target.height
    let fittedSize = sourceRatio > targetRatio
        ? CGSize(width: target.height * sourceRatio, height: target.height)
        : CGSize(width: target.width, height: target.width / sourceRatio)
    let size = CGSize(width: fittedSize.width * zoom, height: fittedSize.height * zoom)
    return CGRect(
        x: target.midX - size.width / 2,
        y: target.midY - size.height / 2,
        width: size.width,
        height: size.height
    )
}

guard (9...11).contains(CommandLine.arguments.count) else {
    fatalError("Usage: composite_hero_frame BASE PLATE OUTPUT X Y WIDTH HEIGHT ZOOM [BORDER] [VERTICAL_SHIFT]")
}

let base = loadImage(CommandLine.arguments[1])
let plate = loadImage(CommandLine.arguments[2])
let outputURL = URL(fileURLWithPath: CommandLine.arguments[3]) as CFURL
let screenX = Double(CommandLine.arguments[4])!
let screenY = Double(CommandLine.arguments[5])!
let screenWidth = Double(CommandLine.arguments[6])!
let screenHeight = Double(CommandLine.arguments[7])!
let zoom = Double(CommandLine.arguments[8])!
let border = CommandLine.arguments.count >= 10 ? Double(CommandLine.arguments[9])! : 0
let verticalShift = CommandLine.arguments.count == 11 ? Double(CommandLine.arguments[10])! : 0
let width = base.width
let height = base.height
let scaleX = Double(width) / 1920
let scaleY = Double(height) / 1080
let screen = CGRect(
    x: screenX * scaleX,
    y: screenY * scaleY,
    width: screenWidth * scaleX,
    height: screenHeight * scaleY
)

guard let context = CGContext(
    data: nil,
    width: width,
    height: height,
    bitsPerComponent: 8,
    bytesPerRow: 0,
    space: CGColorSpaceCreateDeviceRGB(),
    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
) else { fatalError("Unable to create image context") }

context.interpolationQuality = .high
context.draw(base, in: CGRect(x: 0, y: 0, width: width, height: height))
context.saveGState()
context.clip(to: screen)
let plateRect = coverRect(for: plate, in: screen, zoom: zoom).offsetBy(dx: 0, dy: verticalShift * scaleY)
context.draw(plate, in: plateRect)
context.restoreGState()
if border > 0 {
    let scaledBorder = border * scaleX
    context.setStrokeColor(CGColor(red: 0.025, green: 0.035, blue: 0.038, alpha: 0.96))
    context.setLineWidth(scaledBorder)
    context.stroke(screen.insetBy(dx: scaledBorder / 2, dy: scaledBorder / 2))
}

guard let result = context.makeImage(),
      let destination = CGImageDestinationCreateWithURL(outputURL, UTType.jpeg.identifier as CFString, 1, nil) else {
    fatalError("Unable to create output image")
}
CGImageDestinationAddImage(destination, result, [kCGImageDestinationLossyCompressionQuality: 0.88] as CFDictionary)
guard CGImageDestinationFinalize(destination) else { fatalError("Unable to write output image") }
