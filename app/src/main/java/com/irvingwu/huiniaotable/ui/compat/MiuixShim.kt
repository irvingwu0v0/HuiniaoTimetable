package com.irvingwu.huiniaotable.ui.compat

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost as MiuixSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme as MiuixThemeImpl
import top.yukonga.miuix.kmp.theme.TextStyles as MiuixTextStyles

/** 轻量适配层：对外沿用常见 Compose 写法的参数习惯，内部全部渲染为 Miuix 组件。 */


val LocalContentColor = staticCompositionLocalOf { Color.Unspecified }
val LocalTextStyle = staticCompositionLocalOf { TextStyle.Default }

object Shapes {
    val extraSmall: Shape = RoundedCornerShape(8.dp)
    val small: Shape = RoundedCornerShape(12.dp)
    val medium: Shape = RoundedCornerShape(16.dp)
    val large: Shape = RoundedCornerShape(24.dp)
    val extraLarge: Shape = RoundedCornerShape(28.dp)
}

class Typography(styles: MiuixTextStyles) {
    val displayLarge = styles.main
    val displayMedium = styles.main
    val displaySmall = styles.main
    val headlineLarge = styles.title1
    val headlineMedium = styles.title2
    val headlineSmall = styles.title3
    val titleLarge = styles.title2
    val titleMedium = styles.title3
    val titleSmall = styles.title4
    val bodyLarge = styles.body1
    val bodyMedium = styles.body2
    val bodySmall = styles.body2.copy(fontSize = 13.sp)
    val labelLarge = styles.body2
    val labelMedium = styles.body2.copy(fontSize = 12.sp)
    val labelSmall = styles.body2.copy(fontSize = 11.sp)
}

class ColorScheme(colors: top.yukonga.miuix.kmp.theme.Colors) {
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val primaryContainer = colors.primaryContainer
    val onPrimaryContainer = colors.onPrimaryContainer
    val secondary = colors.secondary
    val onSecondary = colors.onSecondary
    val secondaryContainer = colors.secondaryContainer
    val onSecondaryContainer = colors.onSecondaryContainer
    val tertiary = colors.secondaryVariant
    val onTertiary = colors.onSecondaryVariant
    val tertiaryContainer = colors.tertiaryContainer
    val onTertiaryContainer = colors.onTertiaryContainer
    val error = colors.error
    val onError = colors.onError
    val errorContainer = colors.errorContainer
    val onErrorContainer = colors.onErrorContainer
    val background = colors.background
    val onBackground = colors.onBackground
    val surface = colors.surface
    val onSurface = colors.onSurface
    val surfaceVariant = colors.surfaceVariant
    val onSurfaceVariant = colors.onSurfaceSecondary
    val outline = colors.outline
    val outlineVariant = colors.dividerLine
    val surfaceContainerLowest = colors.surface
    val surfaceContainerLow = colors.surfaceContainer
    val surfaceContainer = colors.surfaceContainerHigh
    val surfaceContainerHigh = colors.surfaceContainerHighest
    val surfaceContainerHighest = colors.surfaceContainerHighest
    val inverseSurface = colors.onSurface
    val inverseOnSurface = colors.surface
    val inversePrimary = colors.primary
}

object AppTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = ColorScheme(MiuixThemeImpl.colorScheme)

    val typography: Typography
        @Composable @ReadOnlyComposable get() = Typography(MiuixThemeImpl.textStyles)

    val shapes: Shapes get() = Shapes
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    MiuixText(
        text = text,
        modifier = modifier,
        color = if (color == Color.Unspecified) LocalContentColor.current else color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        letterSpacing = letterSpacing,
        style = style,
    )
}

@Composable
fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    MiuixIcon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.small,
    color: Color = Color.Unspecified,
    contentColor: Color = LocalContentColor.current,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: Any? = null,
    content: @Composable () -> Unit,
) {
    val bg = if (color == Color.Unspecified) AppTheme.colorScheme.surface else color
    Box(modifier = modifier.background(bg, shape)) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

class CardColors(val containerColor: Color, val contentColor: Color)

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = AppTheme.colorScheme.surfaceContainerLow,
        contentColor: Color = AppTheme.colorScheme.onSurface,
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = contentColor,
    ) = CardColors(containerColor, contentColor)

    @Composable
    fun outlinedCardColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = AppTheme.colorScheme.onSurface,
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = contentColor,
    ) = CardColors(containerColor, contentColor)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: Any? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val miuixColors = MiuixCardDefaults.defaultColors(color = colors.containerColor, contentColor = colors.contentColor)
    if (onClick != null) {
        MiuixCard(
            modifier = modifier,
            cornerRadius = 16.dp,
            colors = miuixColors,
            onClick = onClick,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
        }
    } else {
        MiuixCard(modifier = modifier, cornerRadius = 16.dp, colors = miuixColors) {
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
        }
    }
}

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation: Any? = null,
    content: @Composable () -> Unit,
) = Card(modifier = modifier, shape = shape, colors = colors, content = content)

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = AppTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null,
) {
    MiuixSwitch(checked = checked, onCheckedChange = { onCheckedChange?.invoke(it) }, modifier = modifier, enabled = enabled)
}

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null,
) {
    MiuixRadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 2.dp,
    size: Dp = 24.dp,
) {
    MiuixCircularProgressIndicator(modifier = modifier, strokeWidth = strokeWidth, size = size)
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Box(modifier.fillMaxWidth().height(3.dp).background(AppTheme.colorScheme.primary.copy(alpha = 0.35f)))
}

@Composable
fun DropdownArrow(tint: Color) = Icon(AppIcons.ArrowDropDown, null, tint = tint)

@Composable
fun SpacedRow(content: @Composable RowScope.() -> Unit) = Row(verticalAlignment = Alignment.CenterVertically, content = content)

/* ---------- buttons ---------- */

object ButtonDefaults {
    @Composable
    fun buttonColors(containerColor: Color = Color.Unspecified, contentColor: Color = Color.Unspecified) =
        MiuixButtonDefaults.buttonColors()

    @Composable
    fun outlinedButtonColors(containerColor: Color = Color.Unspecified, contentColor: Color = Color.Unspecified) =
        MiuixButtonDefaults.buttonColors()

    @Composable
    fun textButtonColors(containerColor: Color = Color.Unspecified, contentColor: Color = Color.Unspecified) =
        MiuixButtonDefaults.buttonColors()

    @Composable
    fun filledTonalButtonColors(containerColor: Color = Color.Unspecified, contentColor: Color = Color.Unspecified) =
        MiuixButtonDefaults.buttonColors()
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Shapes.small,
    colors: Any? = null,
    elevation: Any? = null,
    contentPadding: Any? = null,
    border: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Shapes.small,
    colors: Any? = null,
    elevation: Any? = null,
    contentPadding: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MiuixTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Shapes.small,
    colors: Any? = null,
    elevation: Any? = null,
    contentPadding: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, AppTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Shapes.small,
    colors: Any? = null,
    elevation: Any? = null,
    contentPadding: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        CompositionLocalProvider(LocalContentColor provides MiuixThemeImpl.colorScheme.onSecondaryVariant) { content() }
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MiuixIconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

/* ---------- scaffold ---------- */

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit,
) {
    MiuixScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

typealias SnackbarHostState = MiuixSnackbarHostState

@Composable
fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    MiuixSnackbarHost(state = hostState, modifier = modifier)
}

/* ---------- dialogs & fields ---------- */

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = Shapes.large,
    containerColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
) {
    top.yukonga.miuix.kmp.window.WindowDialog(show = true, onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon?.invoke()
            title?.invoke()
            text?.invoke()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                Spacer(Modifier.width(8.dp))
                confirmButton()
            }
        }
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    textStyle: TextStyle = AppTheme.typography.bodyLarge,
    colors: Any? = null,
    shape: Shape = Shapes.small,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        label?.invoke()
        Box(contentAlignment = Alignment.CenterStart) {
            top.yukonga.miuix.kmp.basic.TextField(
                value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
                enabled = enabled, readOnly = readOnly, singleLine = singleLine,
                minLines = minLines, maxLines = maxLines, visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, textStyle = textStyle,
                leadingIcon = leadingIcon, trailingIcon = trailingIcon,
            )
            if (value.isEmpty() && placeholder != null) Box(Modifier.padding(horizontal = 16.dp)) { placeholder() }
        }
        supportingText?.invoke()
    }
}

/* ---------- clickable surface / chips ---------- */

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Shapes.small,
    color: Color = Color.Unspecified,
    contentColor: Color = LocalContentColor.current,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: Any? = null,
    content: @Composable () -> Unit,
) {
    val bg = if (color == Color.Unspecified) AppTheme.colorScheme.surface else color
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
private fun ChipBase(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val bg = if (selected) AppTheme.colorScheme.primary else AppTheme.colorScheme.surfaceContainerHighest
    val fg = if (selected) AppTheme.colorScheme.onPrimary else AppTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompositionLocalProvider(LocalContentColor provides fg) {
                leadingIcon?.invoke()
                content()
                trailingIcon?.invoke()
            }
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(50),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
    interactionSource: Any? = null,
) = ChipBase(selected, onClick, modifier, enabled, leadingIcon, trailingIcon) { label() }

@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(50),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
) = ChipBase(false, onClick, modifier, enabled, leadingIcon, trailingIcon) { label() }

@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(50),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
) = ChipBase(false, onClick, modifier, enabled, icon, null) { label() }

object AssistChipDefaults {
    val IconSize: Dp = 18.dp

    @Composable
    fun assistChipColors(containerColor: Color = Color.Unspecified, labelColor: Color = Color.Unspecified) = Unit
}

/* ---------- segmented buttons ---------- */

@Composable
fun SingleChoiceSegmentedButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppTheme.colorScheme.surfaceContainerHighest)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(50),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null,
    border: Any? = null,
    icon: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
) {
    val bg = if (selected) AppTheme.colorScheme.surface else Color.Transparent
    val fg = if (selected) AppTheme.colorScheme.onSurface else AppTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { label() }
    }
}

object SegmentedButtonDefaults {
    @Composable
    fun itemColors() = Unit

    fun itemShape(index: Int, count: Int): Shape = RoundedCornerShape(50)

    @Composable
    fun Icon(active: Boolean) {
        if (active) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/* ---------- list item ---------- */

@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: Any? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.let {
            it()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            overlineContent?.invoke()
            headlineContent()
            supportingContent?.invoke()
        }
        trailingContent?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}

object ListItemDefaults {
    @Composable
    fun colors(containerColor: Color = Color.Unspecified) = Unit
}

/* ---------- dropdown ---------- */

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: Any? = null,
    scrollState: Any? = null,
    content: @Composable () -> Unit,
) {
    if (!expanded) return
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .clip(Shapes.medium)
                .background(AppTheme.colorScheme.surfaceContainer)
                .padding(vertical = 6.dp),
        ) { content() }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: Any? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            it()
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) { text() }
        trailingIcon?.invoke()
    }
}

/* ---------- bottom sheet ---------- */

class SheetState

@Composable
fun rememberModalBottomSheetState(skipPartiallyExpanded: Boolean = false): SheetState = SheetState()

@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = SheetState(),
    containerColor: Color = Color.Unspecified,
    contentColor: Color = LocalContentColor.current,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    dragHandle: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(if (containerColor == Color.Unspecified) AppTheme.colorScheme.surface else containerColor)
                    .padding(bottom = 16.dp),
            ) {
                dragHandle?.invoke()
                CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
            }
        }
    }
}
