package android.example.com.kotlinmessenger.messages

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.adapter.ChatReceiverItemAdapter
import android.example.com.kotlinmessenger.adapter.ChatSenderItemAdapter
import android.example.com.kotlinmessenger.adapter.UserItemAdapter
import android.example.com.kotlinmessenger.model.ChatMessages
import android.example.com.kotlinmessenger.model.User
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import de.hdodenhof.circleimageview.CircleImageView
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ChatLogActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    private var cameraFile: File? = null
    private var currentPath: String? = null
    private var selectedPhotoUri: Uri? = null
    private var selectDocUri: Uri? = null
    lateinit var etTypeMessage: EditText
    lateinit var fabSendMessage: View
    lateinit var imgProfileSender: CircleImageView
    lateinit var txtSenderName: TextView
    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var recyclerViewChatLog: RecyclerView
    lateinit var btnAttach: ImageButton
    lateinit var sharedPreferences: SharedPreferences
    private var bottomSheetAttach: BottomSheetDialog? = null
    private val adapter = GroupAdapter<ViewHolder>()
    private var intentText: String? = ""
    lateinit var progressBar: ProgressBar
    lateinit var rlProgress: RelativeLayout
    lateinit var progressText: TextView
    private var fileName: String? = null
    private val STORAGE_IMAGE_PREMISSION_CODE = 101
    private val STORAGE_DOCS_PREMISSION_CODE = 106
    private val CAMERA_PREMISSION_CODE = 102
    private val OPEN_IMAGE_STORAGE = 103
    private val TAKE_PICTURE = 104
    private val OPEN_DOC_STORAGE = 105
    private val TAG = "ChatLog"
    private var user: User? = null
    private val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        etTypeMessage = findViewById(R.id.etTypeMessage)
        fabSendMessage = findViewById(R.id.fabSendMessage)
        imgProfileSender = findViewById(R.id.imgProfileSender)
        txtSenderName = findViewById(R.id.txtSenderName)
        btnAttach = findViewById(R.id.btnAttach)
        toolbar = findViewById(R.id.toolbar)
        progressText = findViewById(R.id.txtProgressText)
        rlProgress = findViewById(R.id.rlProgress)
        rlProgress.visibility = View.GONE
        progressBar = findViewById(R.id.progressBar3)
        sharedPreferences = getSharedPreferences(getString(R.string.shared_intent), Context.MODE_PRIVATE)
        recyclerViewChatLog = findViewById(R.id.recyclerViewChatLog)
        recyclerViewChatLog.adapter = adapter

        setUpToolbar()

        val chatUser = intent.getParcelableExtra<User>(UserItemAdapter.USER_NAME_KEY)
        user = chatUser
        intentText = intent.getStringExtra(UserItemAdapter.INTENT_TEXT_KEY)

        //if data(text) from another app is being shared, assign it to etTypeMessage
        if (!intentText.isNullOrEmpty()) {
            etTypeMessage.text = Editable.Factory.getInstance().newEditable(intentText)
            //Log.i("test1", intentText!!)
            sharedPreferences.edit().remove(UserItemAdapter.INTENT_ACTION_SEND).apply()
        }

        if (chatUser != null) {
            if (chatUser.profileImageUrl.isNotEmpty()) {
                Glide.with(this@ChatLogActivity).load(chatUser.profileImageUrl).into(imgProfileSender)
            } else {
                imgProfileSender.setImageResource(R.drawable.default_user_image)
            }
            txtSenderName.text = chatUser.username
        } else {
            txtSenderName.text = "Chat"
            imgProfileSender.setImageResource(R.drawable.default_user_image)
        }

        if (chatUser != null) {
            listenForMessages(chatUser.uid)
        } else {
            Toast.makeText(this@ChatLogActivity, "Error!! Please try Again", Toast.LENGTH_SHORT).show()
        }

        btnAttach.setOnClickListener {
            showBottomSheetDialog()
        }

        fabSendMessage.setOnClickListener {
            //send the message
            if (chatUser != null) {
                performSendMessage(chatUser.uid)
                //after message is sent clear text in etTypeMessage
                etTypeMessage.text.clear()
            }
        }

    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showBottomSheetDialog() {
        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.bottom_dialog_attach, null)

        (view.findViewById(R.id.llAttachFromCamera) as View).setOnClickListener {
            cameraPermissionRequest()
            bottomSheetAttach?.dismiss()
        }

        (view.findViewById(R.id.llAttachFromGallery) as View).setOnClickListener {
            storagePermissionRequest()
            bottomSheetAttach?.dismiss()
        }

        (view.findViewById(R.id.llFiles) as View).setOnClickListener {
            storageFilesPermissionRequest()
            bottomSheetAttach?.dismiss()
        }

        bottomSheetAttach = BottomSheetDialog(this)
        bottomSheetAttach?.setContentView(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Objects.requireNonNull(bottomSheetAttach?.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS))
        }

        bottomSheetAttach?.setOnDismissListener {
            bottomSheetAttach = null
        }

        bottomSheetAttach?.show()
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(this, *perms)
    }

    private fun storagePermissionRequest() {
        if (hasStoragePermission()) {
            //open gallery to select photo or video
            browseGallery()
        } else {
            EasyPermissions
                .requestPermissions(
                    this@ChatLogActivity,
                    getString(R.string.rationale_storage),
                    STORAGE_IMAGE_PREMISSION_CODE,
                    *perms
                )
        }
    }

    private fun storageFilesPermissionRequest() {
        if (hasStoragePermission()) {
            //open documents to choose documents
            browseDocs()
        } else {
            EasyPermissions
                .requestPermissions(
                    this@ChatLogActivity,
                    getString(R.string.rationale_storage),
                    STORAGE_DOCS_PREMISSION_CODE,
                    *perms
                )
        }
    }

    private fun browseGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/mp4"))
        startActivityForResult(Intent.createChooser(intent, "Choose video/image"), OPEN_IMAGE_STORAGE)
    }

    private fun browseDocs() {
        val mimeTypes = arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",    // .doc & .docx
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",  // .ppt & .pptx
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",          // .xls & .xlsx
            "text/plain",                                                                 // .txt
            "application/pdf",                                                            // .pdf
            "application/zip",                                                            // .zip
            "application/vnd.android.package-archive"                                     // .apk
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(Intent.createChooser(intent, "Choose File"), OPEN_DOC_STORAGE)
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
                    this@ChatLogActivity,
                    getString(R.string.rationale_camera),
                    CAMERA_PREMISSION_CODE,
                    Manifest.permission.CAMERA
                )
        }
    }

    private  fun createImage(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageName = "IMG_GM_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageName, ".jpg", storageDir)
        fileName = image.name
        currentPath = image.absolutePath
        return image
    }

    private fun listenForMessages(toId: String) {
        val fromId: String = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object: ChildEventListener{
            override fun onCancelled(error: DatabaseError) {

            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessages = snapshot.getValue(ChatMessages::class.java)
                rlProgress.visibility = View.GONE
                if (chatMessages != null && chatMessages.text != "" &&
                    chatMessages.mediaUrl == "" && chatMessages.fileUrl == "") {
                    if (chatMessages.fromId == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatSenderItemAdapter(this@ChatLogActivity,
                            chatMessages.text))
                    } else {
                        adapter.add(ChatReceiverItemAdapter(this@ChatLogActivity,
                            chatMessages.text))
                    }
                } else if (chatMessages != null && chatMessages.fileUrl != "" &&
                    chatMessages.text == "" && chatMessages.mediaUrl == "") {
                    if (chatMessages.fromId == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatSenderItemAdapter(context = this@ChatLogActivity,
                            fileUrl = chatMessages.fileUrl, fileName = chatMessages.fileName))
                    } else {
                        adapter.add(ChatReceiverItemAdapter(context = this@ChatLogActivity,
                            fileUrl = chatMessages.fileUrl, fileName = chatMessages.fileName))
                    }
                } else if (chatMessages != null && chatMessages.fileUrl == "" &&
                    chatMessages.text == "" && chatMessages.mediaUrl != "") {
                    if (chatMessages.fromId == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatSenderItemAdapter(context = this@ChatLogActivity,
                            mediaUrl = chatMessages.mediaUrl, fileName = chatMessages.fileName))
                    } else {
                        adapter.add(ChatReceiverItemAdapter(context = this@ChatLogActivity,
                            mediaUrl = chatMessages.mediaUrl, fileName = chatMessages.fileName))
                    }
                }
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        })
    }

    private fun performSendMessage(toId: String) {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val text = etTypeMessage.text.toString().trim()
        //Prevent blank messages from being sent
        if (text.isBlank()) return

        val fromRef = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toRef = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessages = ChatMessages(fromId = fromId, id = fromRef.key!!, text = text,
            timestamp = System.currentTimeMillis()/1000, toId = toId)

        fromRef.setValue(chatMessages)
            .addOnSuccessListener {
                //scroll to latest message sent
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        toRef.setValue(chatMessages)

        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$fromId/$toId"
        )
        latestMessageFromRef.setValue(chatMessages)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$toId/$fromId"
        )
        latestMessageToRef.setValue(chatMessages)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_IMAGE_STORAGE && data != null && resultCode == Activity.RESULT_OK) {
            //proceed and check which image was selected...
            try {
                selectedPhotoUri = data.data
                val mimeType: String? = data.data?.let { returnUri ->
                    contentResolver.getType(returnUri)
                }
                if (mimeType == "image/jpeg" || mimeType == "image/png" || mimeType == "image/gif") {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val imageName = "IMG_GM_" + timestamp + "_"
                    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val image = File.createTempFile(imageName, ".jpg", storageDir)
                    fileName = image.name
                    image.delete()
                    CropImage.activity(selectedPhotoUri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .setOutputCompressQuality(60)
                        .start(this)
                } else if (mimeType == "video/mp4") {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val imageName = "VID_GM_" + timestamp + "_"
                    val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    val image = File.createTempFile(imageName, ".mp4", storageDir)
                    fileName = image.name
                    image.delete()
                    uploadImageToFirebaseStorage()
                }
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
                    .setCropShape(CropImageView.CropShape.RECTANGLE)
                    .setOutputCompressQuality(60)
                    .start(this)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (requestCode == OPEN_DOC_STORAGE && data != null && resultCode == Activity.RESULT_OK) {
            //proceed and check which image was selected...
            try {
                selectDocUri = data.data
                val file = File(selectDocUri?.path)
                uploadDocToFirebaseStorage(file.name)
                fileName = file.name
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

    private fun uploadDocToFirebaseStorage(fileName: String) {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = user?.uid
        val fromRef = FirebaseStorage.getInstance().getReference("/user-files/$fromId/$toId/$fileName")

        fromRef.putFile(selectDocUri!!)
            .addOnProgressListener {
                val progress: Int = ((100 * it.bytesTransferred) / it.totalByteCount).toInt()
                rlProgress.visibility = View.VISIBLE
                progressBar.progress = progress
                progressText.text = "$progress%"
            }
            .addOnSuccessListener {
                fromRef.downloadUrl
                    .addOnSuccessListener {
                        performSendFiles(it.toString())
                    }
            }
            .removeOnFailureListener {
                Toast.makeText(
                    this,
                    "Some error occurred, please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }

    private fun performSendFiles(fileUrl: String) {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = user?.uid

        //Prevent blank messages from being sent
        if (fileUrl.isBlank()) return

        val fromRef = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toRef = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessages = ChatMessages(fromId = fromId, id = fromRef.key!!,
            fileUrl = fileUrl, fileName = fileName!!,timestamp = System.currentTimeMillis()/1000, toId = toId!!)

        fromRef.setValue(chatMessages)
            .addOnSuccessListener {
                //scroll to latest message sent
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        toRef.setValue(chatMessages)

        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$fromId/$toId"
        )
        latestMessageFromRef.setValue(chatMessages)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$toId/$fromId"
        )
        latestMessageToRef.setValue(chatMessages)
    }

    private fun uploadImageToFirebaseStorage() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = user?.uid
        val fromRef = FirebaseStorage.getInstance().getReference("/user-images/$fromId/$toId/$fileName")

        fromRef.putFile(selectedPhotoUri!!)
            .addOnProgressListener {
                val progress: Int = ((100 * it.bytesTransferred) / it.totalByteCount).toInt()
                rlProgress.visibility = View.VISIBLE
                progressBar.progress = progress
                progressText.text = "$progress%"
            }
            .addOnSuccessListener {
                fromRef.downloadUrl
                    .addOnSuccessListener {
                        performSendMedia(it.toString())
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Some error occurred, please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun performSendMedia(mediaUrl: String) {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = user?.uid

        //Prevent blank messages from being sent
        if (mediaUrl.isBlank()) return

        val fromRef = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toRef = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessages = ChatMessages(fromId = fromId, id = fromRef.key!!,
            mediaUrl = mediaUrl, fileName = fileName!!,timestamp = System.currentTimeMillis()/1000, toId = toId!!)

        fromRef.setValue(chatMessages)
            .addOnSuccessListener {
                //scroll to latest message sent
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        toRef.setValue(chatMessages)

        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$fromId/$toId"
        )
        latestMessageFromRef.setValue(chatMessages)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$toId/$fromId"
        )
        latestMessageToRef.setValue(chatMessages)
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
            STORAGE_IMAGE_PREMISSION_CODE -> {
                //open gallery to select photo or video
                browseGallery()
            }
            STORAGE_DOCS_PREMISSION_CODE -> {
                browseDocs()
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