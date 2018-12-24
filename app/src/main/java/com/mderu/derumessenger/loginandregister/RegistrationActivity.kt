package com.mderu.derumessenger.loginandregister

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mderu.derumessenger.R
import com.mderu.derumessenger.messages.LatestMessagesActivity
import kotlinx.android.synthetic.main.activity_registration.*
import java.util.*

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        register_button_register.setOnClickListener { performRegistration() }
        already_have_account_textview.setOnClickListener {
            Log.d("RegistrationActivity", "Try to show LoginActivity")
            val openLoginActivityIntent = Intent(this, LoginActivity::class.java)
            startActivity(openLoginActivityIntent)
        }
        select_photo_button_register.setOnClickListener {
            Log.d("RegistrationActivity","Try to show photo selector")

            val intentPhotoSelector = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type="image/*"
            startActivityForResult(intentPhotoSelector,0)
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            //Check what the selected image was
            Log.d("RegistrationActivity","Photo was selected")

            //Location Address
            selectedPhotoUri = data.data

            //Bitmap in the above address location
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)

            select_photo_imageview_register.setImageBitmap(bitmap)
            select_photo_button_register.alpha = 0f

        }
    }

    private fun performRegistration() {
         val email = email_edittext_register.text.toString()
         val password = password_edittext_register.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please insert Username/Password", Toast.LENGTH_SHORT).show()
            return
        }
        //FireBase Authentication to Create User with Email and Pass
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                //else if Successful
                Log.d("RegistrationActivity", "Successfully created UID: ${it.result?.user?.uid}")

                uploadImageToFireBaseStorage()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Create User: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
  private fun uploadImageToFireBaseStorage(){
      if(selectedPhotoUri == null) return
      val fileName = UUID.randomUUID().toString()
      val reference = FirebaseStorage.getInstance().getReference("/images/$fileName")

      reference.putFile(selectedPhotoUri!!)
          .addOnSuccessListener {
              Log.d("RegistrationActivity","Image Uploaded Successfully ${it.metadata?.path}")

              reference.downloadUrl.addOnSuccessListener{
                  it.toString()
                  Log.d("RegistrationActivity","File Location: $it")

                  saveUserToFirebaseDatabase(it.toString())
              }
          }
          .addOnFailureListener {
              //do some logging here
          }

    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String){

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(
            uid,
            username_edittext_register.text.toString(),
            profileImageUrl
        )
        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("RegistrationActivity","Finally we see the user to Firebase Database")

                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
    }
}
class User(val uid:String, val username: String, val profileImageUrl: String){
    constructor() : this("","","")
}