// I plugin si dichiarano qui senza applicarli: li applica il modulo che li usa.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
