package com.sg.taskspace

import com.sg.taskspace.util.NotificationActionReceiver
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddParserTest {

    @Test
    fun parseSimpleTitle_defaultsToMediumPriorityAndNoTags() {
        val result = NotificationActionReceiver.parseQuickAddInput("Buy groceries")
        assertEquals("Buy groceries", result.title)
        assertEquals("Medium", result.priority)
        assertEquals("", result.tags)
    }

    @Test
    fun parseHighPriorityAndTags_extractsCorrectly() {
        val result = NotificationActionReceiver.parseQuickAddInput("Review pull request !high #work #dev")
        assertEquals("Review pull request", result.title)
        assertEquals("High", result.priority)
        assertEquals("#work,#dev", result.tags)
    }

    @Test
    fun parseLowPriorityAtStart_extractsCorrectly() {
        val result = NotificationActionReceiver.parseQuickAddInput("!low Pick up laundry #errands")
        assertEquals("Pick up laundry", result.title)
        assertEquals("Low", result.priority)
        assertEquals("#errands", result.tags)
    }

    @Test
    fun parseUrgentAlias_mapsToHighPriority() {
        val result = NotificationActionReceiver.parseQuickAddInput("Server down !urgent #infra")
        assertEquals("Server down", result.title)
        assertEquals("High", result.priority)
        assertEquals("#infra", result.tags)
    }

    @Test
    fun parseOnlyTagsAndPriority_fallsBackToNewTask() {
        val result = NotificationActionReceiver.parseQuickAddInput("!high #urgent")
        assertEquals("New Task", result.title)
        assertEquals("High", result.priority)
        assertEquals("#urgent", result.tags)
    }

    @Test
    fun parseNormalPriority_mapsToMedium() {
        val result = NotificationActionReceiver.parseQuickAddInput("Weekly sync !normal")
        assertEquals("Weekly sync", result.title)
        assertEquals("Medium", result.priority)
        assertEquals("", result.tags)
    }
}
