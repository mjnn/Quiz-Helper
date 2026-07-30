// swift-tools-version: 5.9
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
        .target(name: "AiTrainerCore"),
        .testTarget(
            name: "AiTrainerCoreTests",
            dependencies: ["AiTrainerCore"]
        ),
    ]
)
