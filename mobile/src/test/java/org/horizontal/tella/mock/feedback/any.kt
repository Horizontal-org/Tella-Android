package org.horizontal.tella.mock.feedback

import org.mockito.Mockito

fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

fun <T> anyOrNull(): T = Mockito.any<T>()

