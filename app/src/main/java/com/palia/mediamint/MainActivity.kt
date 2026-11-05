package com.palia.mediamint

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch

private val Mint = Color(0xFF07966F)
private val DeepMint = Color(0xFF087B60)
private val PaleMint = Color(0xFFE7FFF5)
private val Ink = Color(0xFF102A35)
private val Muted = Color(0xFF60737A)
private val Canvas = Color(0xFFF8FCFA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MediaMintTheme {
                MediaMintApp()
            }
        }
    }
}

@Composable
private fun MediaMintTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

private data class MediaTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val mimeTypes: Array<String>,
    val tint: Color,
    val background: Color
)

private val popularTools = listOf(
    MediaTool("Video to MP3", "Extract audio from video", Icons.Default.VideoLibrary, arrayOf("video/*"), Color(0xFF7854D8), Color(0xFFF3EEFF)),
    MediaTool("Video to Image", "Save frames as pictures", Icons.Default.Collections, arrayOf("video/*"), Color(0xFF168DCE), Color(0xFFEBF7FF)),
    MediaTool("Image to PDF", "Create PDF from photos", Icons.Default.Image, arrayOf("image/*"), Color(0xFFE45D63), Color(0xFFFFF0F1)),
    MediaTool("PDF to Image", "Export pages as images", Icons.Default.PictureAsPdf, arrayOf("application/pdf"), Color(0xFF7854D8), Color(0xFFF3EEFF))
)

@Composable
private fun MediaMintApp() {
    var selectedTab by remember { mutableStateOf("Home") }
    var activeTool by remember { mutableStateOf<MediaTool?>(null) }
    var pickedFile by remember { mutableStateOf<Uri?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pickedFile = uri
        if (uri != null) {
            scope.launch {
                snackbar.showSnackbar("File selected. Online conversion service will be connected in the backend phase.")
            }
        }
    }

    fun openTool(tool: MediaTool) {
        activeTool = tool
        filePicker.launch(tool.mimeTypes)
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val tabs = listOf(
                    Triple("Home", Icons.Default.Home, "Home"),
                    Triple("Tools", Icons.Default.Bolt, "Tools"),
                    Triple("Files", Icons.Default.Folder, "Files"),
                    Triple("Pro", Icons.Default.Star, "Pro"),
                    Triple("More", Icons.Default.MoreHoriz, "More")
                )
                tabs.forEach { (label, icon, _) ->
                    NavigationBarItem(
                        selected = selectedTab == label,
                        onClick = { selectedTab = label },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = if (selectedTab == label) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            "Home" -> HomeContent(
                modifier = Modifier.padding(padding),
                onToolClick = ::openTool,
                onAllTools = { selectedTab = "Tools" },
                onWatermark = {
                    openTool(MediaTool("AI Watermark Remover", "Choose an image you own or have permission to edit", Icons.Default.AutoAwesome, arrayOf("image/*"), DeepMint, PaleMint))
                },
                onPro = { selectedTab = "Pro" },
                onNotifications = { showInfo = true }
            )
            "Tools" -> ToolsContent(Modifier.padding(padding), ::openTool)
            "Files" -> SimplePage("Your Files", "Your recent conversions will appear here.", Icons.Default.Folder, Modifier.padding(padding))
            "Pro" -> SimplePage("Palia MediaMint Pro", "Premium plans and limits will be configured after the online service is ready.", Icons.Default.Star, Modifier.padding(padding))
            else -> SimplePage("More", "Settings • Help & Support • About Palia MediaMint", Icons.Default.Settings, Modifier.padding(padding))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Palia MediaMint") },
            text = { Text("All-in-One Media & PDF Tools\n\nPaliaAPK HUB\nDeveloper By shanpalia") },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    onToolClick: (MediaTool) -> Unit,
    onAllTools: () -> Unit,
    onWatermark: () -> Unit,
    onPro: () -> Unit,
    onNotifications: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, top = 12.dp, end = 18.dp, bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Mint, DeepMint))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Palia MediaMint", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("All-in-One Media & PDF Tools", color = Muted, fontSize = 11.sp)
                }
                Surface(color = PaleMint, shape = RoundedCornerShape(50), modifier = Modifier.clickable(onClick = onPro)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Mint, modifier = Modifier.size(16.dp))
                        Text(" Pro", color = DeepMint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Ink,
                    modifier = Modifier.padding(start = 12.dp).size(26.dp).clickable(onClick = onNotifications))
            }
        }
        item {
            HeroBanner()
        }
        item {
            SectionHeader("🔥  Popular Tools", "See All", onAllTools)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                popularTools.forEach { tool ->
                    PopularToolCard(tool, Modifier.weight(1f)) { onToolClick(tool) }
                }
            }
        }
        item {
            SectionHeader("🎯  All Tools")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryCard("Video Tools", "Convert, Compress, Trim, GIF & More", Icons.Default.VideoLibrary, Color(0xFF0D9B6F), Color(0xFFE9FFF5), Modifier.weight(1f)) { onToolClick(popularTools[0]) }
                    CategoryCard("Image Tools", "Resize, Compress, Convert, Edit", Icons.Default.Image, Color(0xFF168DCE), Color(0xFFEEF8FF), Modifier.weight(1f)) { onToolClick(MediaTool("Image Tools", "Choose an image", Icons.Default.Image, arrayOf("image/*"), Color(0xFF168DCE), Color(0xFFEEF8FF))) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryCard("PDF Tools", "Merge, Split, Compress, Rotate", Icons.Default.Description, Color(0xFFE45D63), Color(0xFFFFF0F1), Modifier.weight(1f)) { onToolClick(popularTools[2].copy(title = "PDF Tools", mimeTypes = arrayOf("application/pdf"))) }
                    CategoryCard("AI Photo Tools", "Watermark, Text & Object Remover", Icons.Default.AutoAwesome, Color(0xFF8050C8), Color(0xFFF5EFFF), Modifier.weight(1f), onWatermark)
                }
            }
        }
        item {
            WatermarkFeature(onWatermark)
        }
        item {
            Text("PaliaAPK HUB  •  Developer By shanpalia", color = Muted, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFDDFCEF), Color(0xFFB9F4DE), Color(0xFF42D6A4))))
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxWidth(0.72f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("All-in-One Media,\nImage & PDF Tools", color = Ink, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold)
            FeatureLine("Convert  •  Edit  •  Compress")
            FeatureLine("Remove Watermark")
            FeatureLine("Fast & High Quality")
        }
        Box(Modifier.align(Alignment.CenterEnd).size(76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Mint, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Mint, modifier = Modifier.size(17.dp))
        Text(text, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction))
            Icon(Icons.Default.ChevronRight, null, tint = Mint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PopularToolCard(tool: MediaTool, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(tool.background)
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(tool.icon, null, tint = tool.tint, modifier = Modifier.size(35.dp))
        Text(tool.title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, minLines = 2)
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(background)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(title, color = Ink, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Clip)
            Text(subtitle, color = Ink.copy(alpha = 0.82f), fontSize = 9.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Clip)
        }
        Icon(Icons.Default.ChevronRight, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WatermarkFeature(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE4FFF4), Color(0xFFD0F8E9))))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨ AI Watermark Remover", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Surface(color = Color(0xFFFF5B86), shape = RoundedCornerShape(50)) {
                Text("HOT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        Text("Remove unwanted text, logos and objects from images you own or are authorized to edit.", color = Muted, fontSize = 12.sp)
        Surface(color = Mint, shape = RoundedCornerShape(50)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Choose Image  →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ToolsContent(modifier: Modifier = Modifier, onToolClick: (MediaTool) -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("All Tools", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) }
        item { Text("Choose a tool to select your file.", color = Muted, fontSize = 13.sp) }
        items(popularTools.size) { index ->
            val tool = popularTools[index]
            ToolRow(tool) { onToolClick(tool) }
        }
        item {
            ToolRow(MediaTool("AI Watermark Remover", "Select an image to continue", Icons.Default.AutoAwesome, arrayOf("image/*"), DeepMint, PaleMint)) {
                onToolClick(MediaTool("AI Watermark Remover", "Select an image to continue", Icons.Default.AutoAwesome, arrayOf("image/*"), DeepMint, PaleMint))
            }
        }
    }
}

@Composable
private fun ToolRow(tool: MediaTool, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(tool.background), contentAlignment = Alignment.Center) {
            Icon(tool.icon, null, tint = tool.tint)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(tool.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(tool.subtitle, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Mint)
    }
}

@Composable
private fun SimplePage(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(58.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
