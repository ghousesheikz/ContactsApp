package com.example.contactsapp.ui

import android.Manifest
import android.app.ActionBar
import android.app.Dialog
import android.content.ContentProviderOperation
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Observer
import com.example.contactsapp.R
import com.example.contactsapp.adapter.ContactsAdapter
import com.example.contactsapp.hasPermission
import com.example.contactsapp.requestPermissionWithRationale
import com.example.contactsapp.viewmodel.ContactsViewModel
import kotlinx.android.synthetic.main.activity_contacts.*


class ContactsActivity : AppCompatActivity() {
    private val contactsViewModel by viewModels<ContactsViewModel>()
    private val CONTACTS_READ_REQ_CODE = 100
    private val CONTACTS_WRITE_REQ_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        init()
    }

    private fun init() {
        tvDefault.text = "Fetching contacts!!!"
        val adapter = ContactsAdapter(this)
        rvContacts.adapter = adapter
        contactsViewModel.contactsLiveData.observe(this, Observer {
            tvDefault.visibility = View.GONE
            adapter.contacts = it
        })
        if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            contactsViewModel.fetchContacts()
        } else {
            requestPermissionWithRationale(
                Manifest.permission.READ_CONTACTS, CONTACTS_READ_REQ_CODE, getString(
                    R.string.contact_permission_rationale
                )
            )
        }

        fab_addcontact.setOnClickListener {
            if (hasPermission(Manifest.permission.WRITE_CONTACTS)) {
                createContactDialog()
            } else {
                requestPermissionWithRationale(
                    Manifest.permission.WRITE_CONTACTS, CONTACTS_WRITE_REQ_CODE, getString(
                        R.string.contact_permission_rationale
                    )
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_READ_REQ_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            contactsViewModel.fetchContacts()
        }
    }

    fun createContact(mName: String, mNumber: String) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI
            ).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                    mName
                ).build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mNumber)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                ).build()
        )
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Exception: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun createContactDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)
        val window: Window = dialog.getWindow()!!
        window.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val edt_name = dialog.findViewById(R.id.edt_name) as EditText
        val edt_number = dialog.findViewById(R.id.edt_number) as EditText
        val btn_create = dialog.findViewById(R.id.btn_create) as AppCompatButton
        val btn_cancel = dialog.findViewById(R.id.btn_cancel) as AppCompatButton
        btn_create.setOnClickListener {
            if (!TextUtils.isEmpty(edt_name.text.toString()) && !TextUtils.isEmpty(edt_number.text.toString())) {
                createContact(edt_name.text.toString(), edt_number.text.toString())
                contactsViewModel.fetchContacts()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please Enter Name and Number", Toast.LENGTH_LONG).show()
            }
        }

        btn_cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
