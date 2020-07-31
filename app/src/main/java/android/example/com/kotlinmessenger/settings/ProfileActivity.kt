package android.example.com.kotlinmessenger.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.model.User
import android.net.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import de.hdodenhof.circleimageview.CircleImageView
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ProfileActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks{

    private var cameraFile: File? = null
    private var currentPath: String? = null
    private var selectedPhotoUri: Uri? = null
    lateinit var toolbar: Toolbar
    lateinit var imgProfilePhoto: CircleImageView
    lateinit var txtProfileName: TextView
    lateinit var txtProfileEmail: TextView
    lateinit var txtProfileAbout: TextView
    lateinit var imgBtnEditName: ImageButton
    lateinit var imgBtnEditAbout: ImageButton
    lateinit var fabProfileCamera: FloatingActionButton
    lateinit var llEditName: LinearLayout
    lateinit var llEditAbout: LinearLayout
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var bottomSheetEditName: BottomSheetDialog? = null
    private var bottomSheetEditAbout: BottomSheetDialog? = null
    private val STORAGE_PREMISSION_CODE = 101
    private val CAMERA_PREMISSION_CODE = 102
    private val OPEN_STORAGE = 103
    private val TAKE_PICTURE = 104
    private val TAG = "Profile"
    private val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        //finding ids of different views
        toolbar = findViewById(R.id.toolbarMyProfile)
        imgProfilePhoto = findViewById(R.id.imgMyProfile)
        txtProfileName = findViewById(R.id.txtProfileName)
        txtProfileEmail = findViewById(R.id.txtProfileEmail)
        txtProfileAbout = findViewById(R.id.txtProfileAbout)
        imgBtnEditName = findViewById(R.id.imgBtnEditName)
        imgBtnEditAbout = findViewById(R.id.imgBtnEditAbout)
        fabProfileCamera = findViewById(R.id.fabSelectPhoto)
        llEditName = findViewById(R.id.llEditName)
        llEditAbout = findViewById(R.id.llEditAbout)

        setUpToolbar()

        loadUserProfile()

        fabProfileCamera.setOnClickListener {
            showBottomDialogPick()
        }

        llEditName.setOnClickListener {
            showBottomDialogEditName()
        }

        llEditAbout.setOnClickListener {
            showBottomDialogEditAbout()
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showBottomDialogPick() {
        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.bottom_dialog_pick, null)

        (view.findViewById(R.id.llCamera) as View).setOnClickListener {
            cameraPermissionRequest()
            bottomSheetDialog?.dismiss()
        }

        (view.findViewById(R.id.llGallery) as View).setOnClickListener {
            storagePermissionRequest()
            bottomSheetDialog?.dismiss()
        }

        (view.findViewById(R.id.llRemovePhoto) as View).setOnClickListener {
            removeProfilePhoto()
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog?.setContentView(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Objects.requireNonNull(bottomSheetDialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS))
        }

        bottomSheetDialog?.setOnDismissListener {
            bottomSheetDialog = null
        }

        bottomSheetDialog?.show()
    }

    private fun showBottomDialogEditName() {
        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_name, null)

        (view.findViewById(R.id.etChangeName) as EditText).addTextChangedListener(object : TextWatcher {
            // display the number of characters that can be typed
            override fun afterTextChanged(s: Editable?) {
                val currentText = s.toString()
                val currentLength = 25 - currentText.length
                if (currentLength != 25) {
                    (view.findViewById(R.id.txtCharacterCount) as TextView).text = currentLength.toString()
                } else {
                    (view.findViewById(R.id.txtCharacterCount) as TextView).text = ""
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        (view.findViewById(R.id.btnSave) as View).setOnClickListener {
            val text = (view.findViewById(R.id.etChangeName) as EditText).text.toString().trim()
            if (text.isNotBlank()) {
                updateUserName(text)
                bottomSheetEditName?.dismiss()
            } else {
                Toast.makeText(
                    this,
                    "Name can't be blank!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        (view.findViewById(R.id.btnCancel) as View).setOnClickListener {
            (view.findViewById(R.id.etChangeName) as EditText).text.clear()
            bottomSheetEditName?.dismiss()
        }

        bottomSheetEditName = BottomSheetDialog(this)
        bottomSheetEditName?.setContentView(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Objects.requireNonNull(bottomSheetEditName?.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS))
        }

        bottomSheetEditName?.setOnDismissListener {
            bottomSheetEditName = null
        }

        bottomSheetEditName?.show()
    }

    private fun showBottomDialogEditAbout() {
        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_about, null)

        (view.findViewById(R.id.etChangeAbout) as EditText).addTextChangedListener(object : TextWatcher {
            // display the number of characters that can be typed
            override fun afterTextChanged(s: Editable?) {
                val currentText = s.toString()
                val currentLength = 139 - currentText.length
                if (currentLength != 139) {
                    (view.findViewById(R.id.txtCharacterAboutCount) as TextView).text = currentLength.toString()
                } else {
                    (view.findViewById(R.id.txtCharacterAboutCount) as TextView).text = ""
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        (view.findViewById(R.id.btnSaveAbout) as View).setOnClickListener {
            val text = (view.findViewById(R.id.etChangeAbout) as EditText).text.toString().trim()
            if (text.isNotBlank()) {
                updateUserAbout(text)
                bottomSheetEditAbout?.dismiss()
            } else {
                Toast.makeText(
                    this,
                    "About can't be blank!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        (view.findViewById(R.id.btnCancelAbout) as View).setOnClickListener {
            (view.findViewById(R.id.etChangeAbout) as EditText).text.clear()
            bottomSheetEditAbout?.dismiss()
        }

        bottomSheetEditAbout = BottomSheetDialog(this)
        bottomSheetEditAbout?.setContentView(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Objects.requireNonNull(bottomSheetEditAbout?.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS))
        }

        bottomSheetEditAbout?.setOnDismissListener {
            bottomSheetEditAbout = null
        }

        bottomSheetEditAbout?.show()
    }

    private fun removeProfilePhoto() {
        val dialog = AlertDialog.Builder(this@ProfileActivity)
        dialog.setTitle("Remove Photo")
        dialog.setMessage("Are you sure you want to remove profile photo?")
        dialog.setPositiveButton("REMOVE") {dlg, listener ->
            //Remove profile photo
            val filename = "profile_image"
            val uid = FirebaseAuth.getInstance().uid
            val ref = FirebaseStorage.getInstance().getReference("/images/$uid/$filename")
            ref.delete()
                .addOnSuccessListener {
                    //All user profile images deleted
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profile photo removed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    //updating photo value as empty in user database
                    val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
                    userRef.child("profileImageUrl")
                        .setValue("").addOnSuccessListener {
                            imgProfilePhoto.setImageResource(R.drawable.default_user_image)
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Some error occurred! Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    val uid = FirebaseAuth.getInstance().uid
                    val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onCancelled(error: DatabaseError) {}

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null && user.profileImageUrl.isEmpty()) {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "No profile photo to be removed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Oops an error occurred. Try again",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                }
        }
        dialog.setNegativeButton("CANCEL") {dlg, listener ->
            dlg.dismiss()
        }
        dialog.create()
        dialog.show()
    }

    private fun updateUserName(text: String) {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.child("username").setValue(text).addOnSuccessListener {
            txtProfileName.text = text
            Toast.makeText(
                this,
                "Name updated!!",
                Toast.LENGTH_SHORT
            ).show()
        }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Some error occurred! Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUserAbout(text: String) {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.child("about").setValue(text).addOnSuccessListener {
            txtProfileAbout.text = text
            Toast.makeText(
                this,
                "Updated!!",
                Toast.LENGTH_SHORT
            ).show()
        }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Some error occurred! Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(this, *perms)
    }

    private fun storagePermissionRequest() {
        if (hasStoragePermission()) {
            //open gallery to select photo
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivityForResult(intent, OPEN_STORAGE)
        } else {
            EasyPermissions
                .requestPermissions(
                    this@ProfileActivity,
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
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivityForResult(intent, TAKE_PICTURE)
                }
            }
        } else {
            //Request for permission
            EasyPermissions
                .requestPermissions(
                    this@ProfileActivity,
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
                CropImage.activity(selectedPhotoUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        if (requestCode == TAKE_PICTURE && data != null && resultCode == Activity.RESULT_OK) {
            try {
                cameraFile = File(currentPath)
                selectedPhotoUri = Uri.fromFile(cameraFile)
                CropImage.activity(selectedPhotoUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            //Do something after user returned from app settings screen
            Log.d(TAG, "Returned from application settings")
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK){
                selectedPhotoUri = result.uri
                uploadImageToFirebaseStorage()
                if (cameraFile != null) {
                    cameraFile?.delete()
                    cameraFile = null
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Toast.makeText(
                    this,
                    "Some error occurred, please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().uid
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(user.profileImageUrl)
                            .into(imgProfilePhoto)
                    } else {
                        imgProfilePhoto.setImageResource(R.drawable.default_user_image)
                    }
                    txtProfileName.text = user.username
                    txtProfileEmail.text = currentUser?.email
                    txtProfileAbout.text = user.about
                }
            }

        })
    }

    private fun uploadImageToFirebaseStorage() {

        val uid = FirebaseAuth.getInstance().uid
        if (selectedPhotoUri != null) {
            val filename = "profile_image"
            val ref = FirebaseStorage.getInstance().getReference("/images/$uid/$filename")

            //upload image to firebase storage
            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    imgProfilePhoto.setImageURI(selectedPhotoUri)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profile photo updated.",
                        Toast.LENGTH_SHORT
                    ).show()

                    //send link generated to the user database
                    ref.downloadUrl
                        .addOnSuccessListener {
                            addImagetoUserDatabase(it.toString())
                        }

                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Upload Failed. Please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        } else {
            Toast.makeText(
                this@ProfileActivity,
                "Please Try Again",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addImagetoUserDatabase(profileImageUrl: String) {

        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        //update the profile image url
        ref.child("profileImageUrl").setValue(profileImageUrl)
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