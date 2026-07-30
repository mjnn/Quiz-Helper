package com.aitrainer.practice.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BankMergeTest {

    @Test
    fun bankKind_hasExpectedDisplayNames() {
        assertEquals("人工智能训练师（三级）理论题-单选", BankKind.SINGLE.displayName)
        assertEquals("人工智能训练师（三级）理论题-判断", BankKind.JUDGE.displayName)
    }
}
