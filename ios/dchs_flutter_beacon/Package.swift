// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "dchs_flutter_beacon",
    platforms: [
        .iOS("13.0"),
    ],
    products: [
        .library(
            name: "dchs-flutter-beacon",
            targets: ["dchs_flutter_beacon"]
        ),
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework"),
    ],
    targets: [
        .target(
            name: "dchs_flutter_beacon",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
            ],
            resources: [
                .process("PrivacyInfo.xcprivacy"),
            ],
            cSettings: [
                .headerSearchPath("include/dchs_flutter_beacon"),
            ]
        ),
    ]
)
