package com.mderu.derumessenger.messages

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.mderu.derumessenger.R
import com.mderu.derumessenger.models.ChatMessage
import com.mderu.derumessenger.models.User
import com.mderu.derumessenger.views.ChatFromItem
import com.mderu.derumessenger.views.ChatToItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*


class ChatLogActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ChatLog"
    }

    val adapter = GroupAdapter<ViewHolder>()

    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recycler_view_chat_log.adapter = adapter

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username

        listenForMessage()

        send_button_chat_log.setOnClickListener {
            Log.d(TAG,"Attempt to send the message")
            performSendMessage()
        }
    }

    private fun listenForMessage(){
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-mesages/$fromId/$toId")


        ref.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessagesActivity.currentUser ?: return

                        adapter.add(ChatFromItem(chatMessage.text, currentUser))
                    } else {
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }
                }

                recycler_view_chat_log.scrollToPosition(adapter.itemCount -1)
            }

            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }
        })
    }

    private fun performSendMessage(){

       val text =  edittext_chat_log.text.toString()

        val fromId = FirebaseAuth.getInstance().uid
        val user =  intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user.uid

        if(fromId == null)return



        val fromReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(fromReference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000 )

        fromReference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message ${fromReference.key}")
                edittext_chat_log.text.clear()
                recycler_view_chat_log.scrollToPosition(adapter.itemCount -1)
            }
        toReference.setValue(chatMessage)

        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageFromRef.setValue(chatMessage)
        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }
}


