package com.sg.taskspace.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TagStyle(
    val color: Color,
    val icon: ImageVector
)

object TagStyleHelper {

    fun getStyleForTag(tag: String): TagStyle {
        val cleanTag = tag.trim().lowercase().removePrefix("#")
        return when {
            cleanTag.contains("work") || cleanTag.contains("job") || cleanTag.contains("office") ->
                TagStyle(Color(0xFF6366F1), Icons.Default.Work) // Indigo
            cleanTag.contains("health") || cleanTag.contains("gym") || cleanTag.contains("fitness") ->
                TagStyle(Color(0xFF10B981), Icons.Default.Favorite) // Emerald
            cleanTag.contains("study") || cleanTag.contains("school") || cleanTag.contains("book") ->
                TagStyle(Color(0xFFF59E0B), Icons.Default.School) // Amber
            cleanTag.contains("finance") || cleanTag.contains("money") || cleanTag.contains("bank") ->
                TagStyle(Color(0xFF14B8A6), Icons.Default.AttachMoney) // Teal
            cleanTag.contains("urgent") || cleanTag.contains("important") || cleanTag.contains("asap") ->
                TagStyle(Color(0xFFEF4444), Icons.Default.PriorityHigh) // Rose
            cleanTag.contains("design") || cleanTag.contains("art") || cleanTag.contains("ui") ->
                TagStyle(Color(0xFFEC4899), Icons.Default.Palette) // Pink
            cleanTag.contains("personal") || cleanTag.contains("home") || cleanTag.contains("life") ->
                TagStyle(Color(0xFF8B5CF6), Icons.Default.Person) // Violet
            cleanTag.contains("quick") || cleanTag.contains("win") ->
                TagStyle(Color(0xFF0EA5E9), Icons.Default.FlashOn) // Sky
            cleanTag.contains("research") ->
                TagStyle(Color(0xFF84CC16), Icons.Default.Search) // Lime
            cleanTag.contains("meeting") || cleanTag.contains("call") ->
                TagStyle(Color(0xFFF97316), Icons.Default.Groups) // Orange
            else ->
                TagStyle(Color(0xFF6366F1), Icons.AutoMirrored.Filled.Label)
        }
    }
}
