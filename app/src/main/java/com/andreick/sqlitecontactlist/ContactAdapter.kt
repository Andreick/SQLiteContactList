package com.andreick.sqlitecontactlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andreick.sqlitecontactlist.databinding.ItemContactBinding

class ContactAdapter(
    private val contactList: List<Contact>,
    private val onEditContactListener: (id: Int) -> Unit,
    private val onDeleteContactListener: (id: Int) -> Unit,
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemContactBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactList[position]
        with(holder.binding) {
            tvContactName.text = contact.name
            tvContactPhone.text = contact.phone
            ivEditContact.setOnClickListener { onEditContactListener(contact.id) }
            ivDeleteContact.setOnClickListener { onDeleteContactListener(contact.id) }
        }
    }

    override fun getItemCount(): Int = contactList.size
}