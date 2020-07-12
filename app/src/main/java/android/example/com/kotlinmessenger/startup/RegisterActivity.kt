package android.example.com.kotlinmessenger.startup

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.messages.LatestMessagesActivity
import android.example.com.kotlinmessenger.model.User
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    var currentPath: String? = null

    lateinit var txtHaveAccount: TextView
    lateinit var btnRegister: Button
    lateinit var etUserName: EditText
    lateinit var etEmail: EditText
    lateinit var etPassword: EditText
    lateinit var btnSelectPhoto: Button
    lateinit var imgSelectPhoto: CircleImageView
    var selectedPhotoUri: Uri? = null
    lateinit var progressLayout: RelativeLayout
    lateinit var progressBar: ProgressBar
    private val TAG = "Register"
    private val STORAGE_PREMISSION_CODE = 101
    private val CAMERA_PREMISSION_CODE = 102
    private val OPEN_STORAGE = 103
    private val TAKE_PICTURE = 104
    private val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //finding the respective view Ids
        txtHaveAccount = findViewById(R.id.txtStatic2)
        btnRegister = findViewById(R.id.btnRegister)
        etUserName = findViewById(R.id.etUserName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        imgSelectPhoto = findViewById(R.id.imgSelectPhoto)
        progressLayout = findViewById(R.id.progressLayout2)
        progressBar = findViewById(R.id.progressBar2)

        progressLayout.visibility = View.GONE

        //Go back to Sign in if user has Account
        txtHaveAccount.setOnClickListener {
            onBackPressed()
            //makes the transition from one activity to the other disappear
            overridePendingTransition(0, 0)
        }

        //button to upload user profile photo to firebase database
        btnSelectPhoto.setOnClickListener {
            selectPhoto()
        }

        //Registration of new User on clicking the REGISTER Button
        btnRegister.setOnClickListener {
            performRegistration()
        }

    }

    private fun selectPhoto() {
        val options = arrayOf<CharSequence>("Camera", "Gallery", "Cancel")
        val optionDialog = AlertDialog.Builder(this@RegisterActivity)
        optionDialog.setTitle("Add Photo!")
        optionDialog.setItems(options, DialogInterface.OnClickListener { dialog, item ->
            if (options[item].equals("Camera")) {
                cameraPermissionRequest()
            } else if (options[item].equals("Gallery")) {
                storagePermissionRequest()
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss()
            }
        })
        optionDialog.show()
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(this, *perms)
    }

    private fun storagePermissionRequest() {
        if (hasStoragePermission()) {
            //open gallery to select photo
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, OPEN_STORAGE)
        } else {
            EasyPermissions
                .requestPermissions(
                    this@RegisterActivity,
                    getString(R.string.rationale_storage),
                    STORAGE_PREMISSION_CODE,
                    *perms
                )
        }
    }

    private fun hasCameraPermission(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)
    }

    private fun cameraPermissionRequest() {
        if (hasCameraPermission()) {
            //open camera to take photo
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImage()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (photoFile != null) {
                    val photoUri = FileProvider.getUriForFile(this,
                        "android.example.com.kotlinmessenger.fileprovider",
                        photoFile)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(intent, TAKE_PICTURE)
                }
            }
        } else {
            //Request for permission
            EasyPermissions
                .requestPermissions(
                    this@RegisterActivity,
                    getString(R.string.rationale_camera),
                    CAMERA_PREMISSION_CODE,
                    Manifest.permission.CAMERA
                )
        }
    }

    private  fun createImage(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageName, ".jpg", storageDir)
        currentPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_STORAGE && data != null && resultCode == Activity.RESULT_OK) {
            //proceed and check which image was selected...
            try {
                selectedPhotoUri = data.data
                imgSelectPhoto.setImageURI(selectedPhotoUri)
                btnSelectPhoto.alpha = 0f
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        if (requestCode == TAKE_PICTURE && data != null && resultCode == Activity.RESULT_OK) {
            try {
                val file = File(currentPath)
                selectedPhotoUri = Uri.fromFile(file)
                imgSelectPhoto.setImageURI(selectedPhotoUri)
                btnSelectPhoto.alpha = 0f
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            //Do something after user returned from app settings screen
            Log.d(TAG, "Returned from application settings")
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        //overridePendingTransition(0, 0)
    }

    private fun performRegistration() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        val username = etUserName.text.toString()

        //if details are filled and button is clicked display a toast
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(
                this@RegisterActivity,
                "Please fill in all the details",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        //progress bar displayed till authentication is complete
        btnSelectPhoto.visibility = View.GONE
        btnRegister.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE

        //Firebase Authentication to create user with email and password
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                //If creation of new user is unsuccessful
                if (!it.isSuccessful)
                    return@addOnCompleteListener
                else {
                    //If creation of new user was successful
                    Log.d(TAG, "User is now Registered!!")
                    uploadImageToFirebaseStorage()
                }
            }
            .addOnFailureListener {
                btnSelectPhoto.visibility = View.VISIBLE
                btnRegister.visibility = View.VISIBLE
                progressLayout.visibility = View.GONE
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration Failed : ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "User registration failed : ${it.message}")
            }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) {
            saveUserToFirebaseDatabase("")
            return
        }

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        //upload image to firebase storage
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d(TAG, "Photo uploaded Successfully : ${it.metadata?.path}")

                ref.downloadUrl
                    .addOnSuccessListener {
                        Log.d(TAG, "File Location : $it")

                        saveUserToFirebaseDatabase(it.toString())
                    }
            }
            .addOnFailureListener {
                //do some task
                btnSelectPhoto.visibility = View.VISIBLE
                btnRegister.visibility = View.VISIBLE
                progressLayout.visibility = View.GONE

                //delete account if image failed to upload
                val user = FirebaseAuth.getInstance().currentUser!!
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registration Failed! Please Try Again", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "User account deleted.")
                        }
                    }
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(
            uid,
            etUserName.text.toString(),
            profileImageUrl
        )
        ref.setValue(user).addOnSuccessListener {
            Log.d(TAG, "User registered to Firebase Database")
            btnSelectPhoto.visibility = View.VISIBLE
            btnRegister.visibility = View.VISIBLE
            progressLayout.visibility = View.GONE
            val intent = Intent(this@RegisterActivity, LatestMessagesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

        }
            .addOnFailureListener {
                Log.d(TAG, "${it.message}")
                btnSelectPhoto.visibility = View.VISIBLE
                btnRegister.visibility = View.VISIBLE
                progressLayout.visibility = View.GONE

                //delete account if user failed to upload to database
                val currentUser = FirebaseAuth.getInstance().currentUser!!
                currentUser.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "User account deleted.")
                        }
                    }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //forwarding results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size)

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms))
        {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size)
        when (requestCode) {
            CAMERA_PREMISSION_CODE -> {
                //open camera to take photo
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImage()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    if (photoFile != null) {
                        val photoUri = FileProvider.getUriForFile(this,
                            "android.example.com.kotlinmessenger.fileprovider",
                            photoFile)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        startActivityForResult(intent, TAKE_PICTURE)
                    }
                }
            }
            STORAGE_PREMISSION_CODE -> {
                //open gallery to select photo
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, 103)
            }
        }
    }

    override fun onRationaleAccepted(requestCode:Int) {
        Log.d(TAG, "onRationaleAccepted:" + requestCode)
    }
    override fun onRationaleDenied(requestCode:Int) {
        Log.d(TAG, "onRationaleDenied:" + requestCode)
    }

}
