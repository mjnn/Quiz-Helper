// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "AiTrainerCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        .library(name: "AiTrainerCore", targets: ["AiTrainerCore"]),
    ],
    targets: [
        .target(
            name: "AiTrainerCore",
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ]
        ),
        .testTarget(
            name: "AiTrainerCoreTests",
            dependencies: ["AiTrainerCore"],
            swiftSettings: [
                .swiftLanguageMode(.v5),
            ]
        ),
    ]
)
