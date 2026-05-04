package org.angryscan.app.ui.windows.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MainScreenAdaptiveScale(
    val scale: Float,
) {
    fun dp(base: Dp, min: Dp = base, max: Dp = base * 1.6f): Dp =
        (base * scale).coerceIn(min, max)

    fun sp(base: TextUnit, min: TextUnit = base, max: TextUnit = base * 1.45f): TextUnit {
        val scaled = base.value * scale
        val clamped = scaled.coerceIn(min.value, max.value)
        return clamped.sp
    }
}

@Immutable
data class MainSourceLayoutTokens(
    val controlHeight: Dp,
    val controlCorner: Dp,
    val controlGapCompact: Dp,
    val controlGapRegular: Dp,
    val pathMinWidthCompact: Dp,
    val pathMinWidthRegular: Dp,
    val scanButtonWidthCompact: Dp,
    val scanButtonWidthRegular: Dp,
    val scanButtonWidthWide: Dp,
    val fieldMinHeight: Dp,
    val inlinePaddingHorizontal: Dp,
    val inlinePaddingVertical: Dp,
    val inlineControlGap: Dp,
    val iconButtonSize: Dp,
    val iconSize: Dp,
    val compactFieldHeight: Dp,
    val compactFieldCorner: Dp,
)

@Immutable
data class MainSettingsTokens(
    val groupLabelWidth: Dp,
    val headerInlineActionSpacing: Dp,
    val contentBelowHeaderSpacing: Dp,
    val sectionCardCorner: Dp,
    val sectionHeaderPadding: Dp,
    val sectionContentPadding: Dp,
    val sectionHeaderAlignOffset: Dp,
)

@Immutable
data class MainScanListTokens(
    val finishedColumnWidth: Dp,
    val durationColumnWidth: Dp,
    val statusColumnWidth: Dp,
    val objectSizeColumnWidth: Dp,
    val piiFoundColumnWidth: Dp,
    val piiSizeColumnWidth: Dp,
    val piiScoreColumnWidth: Dp,
    val attributesColumnWidth: Dp,
    val chevronColumnWidth: Dp,
    val rowHorizontalPadding: Dp,
    val rowMainToMetricsGap: Dp,
    val rowCorner: Dp,
)

@Immutable
data class MainScreenAdaptiveTokens(
    val scale: MainScreenAdaptiveScale,
    val verticalScale: MainScreenAdaptiveScale,
    val source: MainSourceLayoutTokens,
    val settings: MainSettingsTokens,
    val scanList: MainScanListTokens,
)

val LocalMainScreenAdaptiveTokens = compositionLocalOf {
    buildMainScreenAdaptiveTokens(
        widthScale = MainScreenAdaptiveScale(scale = 1f),
        heightScale = MainScreenAdaptiveScale(scale = 1f),
    )
}

@Composable
fun rememberMainScreenAdaptiveTokens(maxWidth: Dp, maxHeight: Dp): MainScreenAdaptiveTokens {
    val widthScaleFactor = remember(maxWidth) {
        val rawScale = (maxWidth.value / 1280f).coerceIn(1f, 1.55f)
        MainScreenAdaptiveScale(scale = rawScale)
    }
    val heightScaleFactor = remember(maxHeight) {
        val rawScale = (maxHeight.value / 720f).coerceIn(1f, 1.35f)
        MainScreenAdaptiveScale(scale = rawScale)
    }
    return remember(widthScaleFactor, heightScaleFactor) {
        buildMainScreenAdaptiveTokens(widthScale = widthScaleFactor, heightScale = heightScaleFactor)
    }
}

@Composable
fun rememberMainSourceRowTokens(maxWidth: Dp, maxHeight: Dp): MainSourceLayoutTokens {
    val widthAdaptive = LocalMainScreenAdaptiveTokens.current.scale
    val heightAdaptive = LocalMainScreenAdaptiveTokens.current.verticalScale
    val scanButtonScale = ((widthAdaptive.scale - 1f) * 0.82f) + 1f
    return remember(maxWidth, maxHeight, widthAdaptive, heightAdaptive, scanButtonScale) {
        MainSourceLayoutTokens(
            controlHeight = heightAdaptive.dp(68.dp, min = 64.dp, max = 86.dp),
            controlCorner = widthAdaptive.dp(18.dp, min = 16.dp, max = 24.dp),
            controlGapCompact = widthAdaptive.dp(8.dp, min = 8.dp, max = 14.dp),
            controlGapRegular = widthAdaptive.dp(12.dp, min = 12.dp, max = 20.dp),
            pathMinWidthCompact = widthAdaptive.dp(460.dp, min = 460.dp, max = 680.dp),
            pathMinWidthRegular = widthAdaptive.dp(500.dp, min = 500.dp, max = 760.dp),
            scanButtonWidthCompact = ((220.dp * 0.75f) * scanButtonScale).coerceIn(165.dp, 290.dp),
            scanButtonWidthRegular = ((232.dp * 0.75f) * scanButtonScale).coerceIn(174.dp, 305.dp),
            scanButtonWidthWide = ((240.dp * 0.75f) * scanButtonScale).coerceIn(180.dp, 320.dp),
            fieldMinHeight = heightAdaptive.dp(44.dp, min = 42.dp, max = 52.dp),
            inlinePaddingHorizontal = widthAdaptive.dp(12.dp, min = 10.dp, max = 20.dp),
            inlinePaddingVertical = heightAdaptive.dp(8.dp, min = 7.dp, max = 11.dp),
            inlineControlGap = widthAdaptive.dp(10.dp, min = 8.dp, max = 16.dp),
            iconButtonSize = heightAdaptive.dp(40.dp, min = 36.dp, max = 46.dp),
            iconSize = heightAdaptive.dp(22.dp, min = 20.dp, max = 26.dp),
            compactFieldHeight = heightAdaptive.dp(32.dp, min = 30.dp, max = 38.dp),
            compactFieldCorner = widthAdaptive.dp(10.dp, min = 8.dp, max = 14.dp),
        )
    }
}

private fun buildMainScreenAdaptiveTokens(
    widthScale: MainScreenAdaptiveScale,
    heightScale: MainScreenAdaptiveScale,
): MainScreenAdaptiveTokens {
    val settingsScale = ((widthScale.scale - 1f) * 0.72f) + 1f
    val settingsAdaptive = MainScreenAdaptiveScale(scale = settingsScale)
    val scanListScale = ((widthScale.scale - 1f) * 0.78f) + 1f
    val scanListAdaptive = MainScreenAdaptiveScale(scale = scanListScale)

    return MainScreenAdaptiveTokens(
        scale = widthScale,
        verticalScale = heightScale,
        source = MainSourceLayoutTokens(
            controlHeight = heightScale.dp(68.dp, min = 64.dp, max = 86.dp),
            controlCorner = widthScale.dp(18.dp, min = 16.dp, max = 24.dp),
            controlGapCompact = widthScale.dp(8.dp, min = 8.dp, max = 14.dp),
            controlGapRegular = widthScale.dp(12.dp, min = 12.dp, max = 20.dp),
            pathMinWidthCompact = widthScale.dp(460.dp, min = 460.dp, max = 680.dp),
            pathMinWidthRegular = widthScale.dp(500.dp, min = 500.dp, max = 760.dp),
            scanButtonWidthCompact = 165.dp,
            scanButtonWidthRegular = 174.dp,
            scanButtonWidthWide = 180.dp,
            fieldMinHeight = heightScale.dp(44.dp, min = 42.dp, max = 52.dp),
            inlinePaddingHorizontal = widthScale.dp(12.dp, min = 10.dp, max = 20.dp),
            inlinePaddingVertical = heightScale.dp(8.dp, min = 7.dp, max = 11.dp),
            inlineControlGap = widthScale.dp(10.dp, min = 8.dp, max = 16.dp),
            iconButtonSize = heightScale.dp(40.dp, min = 36.dp, max = 46.dp),
            iconSize = heightScale.dp(22.dp, min = 20.dp, max = 26.dp),
            compactFieldHeight = heightScale.dp(32.dp, min = 30.dp, max = 38.dp),
            compactFieldCorner = widthScale.dp(10.dp, min = 8.dp, max = 14.dp),
        ),
        settings = MainSettingsTokens(
            groupLabelWidth = settingsAdaptive.dp(140.dp, min = 136.dp, max = 220.dp),
            headerInlineActionSpacing = settingsAdaptive.dp(12.dp, min = 10.dp, max = 18.dp),
            contentBelowHeaderSpacing = settingsAdaptive.dp(6.dp, min = 5.dp, max = 10.dp),
            sectionCardCorner = settingsAdaptive.dp(12.dp, min = 10.dp, max = 18.dp),
            sectionHeaderPadding = settingsAdaptive.dp(14.dp, min = 12.dp, max = 22.dp),
            sectionContentPadding = settingsAdaptive.dp(14.dp, min = 12.dp, max = 22.dp),
            sectionHeaderAlignOffset = settingsAdaptive.dp(14.dp, min = 12.dp, max = 22.dp),
        ),
        scanList = MainScanListTokens(
            finishedColumnWidth = scanListAdaptive.dp(92.dp, min = 86.dp, max = 130.dp),
            durationColumnWidth = scanListAdaptive.dp(104.dp, min = 98.dp, max = 146.dp),
            statusColumnWidth = scanListAdaptive.dp(110.dp, min = 102.dp, max = 160.dp),
            objectSizeColumnWidth = scanListAdaptive.dp(98.dp, min = 92.dp, max = 138.dp),
            piiFoundColumnWidth = scanListAdaptive.dp(92.dp, min = 86.dp, max = 130.dp),
            piiSizeColumnWidth = scanListAdaptive.dp(98.dp, min = 92.dp, max = 138.dp),
            piiScoreColumnWidth = scanListAdaptive.dp(92.dp, min = 86.dp, max = 130.dp),
            attributesColumnWidth = scanListAdaptive.dp(232.dp, min = 210.dp, max = 340.dp),
            chevronColumnWidth = scanListAdaptive.dp(56.dp, min = 52.dp, max = 86.dp),
            rowHorizontalPadding = scanListAdaptive.dp(12.dp, min = 10.dp, max = 18.dp),
            rowMainToMetricsGap = scanListAdaptive.dp(12.dp, min = 10.dp, max = 18.dp),
            rowCorner = scanListAdaptive.dp(12.dp, min = 10.dp, max = 18.dp),
        )
    )
}

