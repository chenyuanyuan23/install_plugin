// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "install_plugin",
    platforms: [
        .iOS("12.0")
    ],
    products: [
        .library(name: "install-plugin", targets: ["install_plugin"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "install_plugin",
            dependencies: [],
            resources: []
        )
    ]
)
