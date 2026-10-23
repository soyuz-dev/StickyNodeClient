plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}


val deploymentDir = file(
    "${System.getProperty("user.home")}/stickynodes-deploy"
)

val stageWeb by tasks.registering(Sync::class) {
    group = "deployment"
    description = "Copy the production web build into the Vercel deployment directory"

    dependsOn(":webApp:jsBrowserDistribution")

    from(project(":webApp").layout.buildDirectory.dir("dist/js/productionExecutable")) {
        exclude(".vercel/**")
    }

    into(deploymentDir)

    preserve {
        include(".vercel")
        include(".vercel/**")
    }
}

tasks.register<Exec>("deployWeb") {
    group = "deployment"
    description = "Build and deploy StickyNodes to Vercel"

    dependsOn(stageWeb)

    workingDir = deploymentDir

    commandLine("cmd", "/c", "vercel", "--prod")
}
