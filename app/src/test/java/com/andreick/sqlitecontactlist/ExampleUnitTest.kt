package com.andreick.sqlitecontactlist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.andreick.sqlitecontactlist.database.DbHelper
import com.andreick.sqlitecontactlist.repository.ContactListRepository
import com.andreick.sqlitecontactlist.viewmodel.ContactListViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    lateinit var repository: ContactListRepository
    lateinit var viewModel: ContactListViewModel

    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        repository = ContactListRepository(DbHelper(appContext))
    }

    private fun mockRepository() {
        repository = Mockito.mock(ContactListRepository::class.java)
        Mockito.`when`(repository.searchContacts(
            Mockito.anyString(),
            MockitoHelper.anyObject(),
            MockitoHelper.anyObject()
        )).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            val onSuccess = it.arguments[1] as? (List<Contact>) -> Unit
            val contactList = mutableListOf<Contact>().apply {
                add(Contact(1, "Unit Test", "123456789"))
            }
            onSuccess?.invoke(contactList)
            ""
        }
    }

    @Test
    fun viewModelTest() {
        mockRepository()
        viewModel = ContactListViewModel(repository)
        var contactList: List<Contact>? = null
        val lock = CountDownLatch(1)
        repository.searchContacts("", { searchedContacts ->
            contactList = searchedContacts
            lock.countDown()
        }, { e->
            fail("${e.message}")
        })
        lock.await(3000, TimeUnit.MILLISECONDS)
        assertNotNull(contactList)
        assertTrue(contactList!!.isNotEmpty())
        assertEquals(1, contactList!!.size)
    }

    @Test
    fun repositoryTest() {
        var contactList: List<Contact>? = null
        val lock = CountDownLatch(1)
        repository.searchContacts("", { searchedContacts ->
            contactList = searchedContacts
            lock.countDown()
        }, { e->
            fail("${e.message}")
        })
        lock.await(3000, TimeUnit.MILLISECONDS)
        assertNotNull(contactList)
        assertTrue(contactList!!.isNotEmpty())
        assertEquals(2, contactList!!.size)
    }

    object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }
        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T
    }
}