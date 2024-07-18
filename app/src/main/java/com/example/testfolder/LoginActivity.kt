package com.example.testfolder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingImage: ImageView
    private lateinit var loadingText: TextView
    private lateinit var rotateAnimation: Animation
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
        private const val NOTIFICATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Google Sign-In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google 계정 로그아웃
        googleSignInClient.signOut().addOnCompleteListener {
            // 로그아웃 후 작업을 수행할 수 있음
        }

        val loginBtn = findViewById<Button>(R.id.login_btn)
        val findIdPage = findViewById<TextView>(R.id.find_id_tv)
        val findPwPage = findViewById<TextView>(R.id.find_pw_tv)
        val registerPage = findViewById<TextView>(R.id.register_tv)
//        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.btn_google_sign_in) //구글 버튼 보류
        val inputPw = findViewById<TextInputEditText>(R.id.input_pw)
        val inputId = findViewById<TextInputEditText>(R.id.input_id)

        // 디버깅을 위해 초기화 확인 로그 추가
        Log.d(TAG, "onCreate: inputPw initialized = ${inputPw != null}")

        // 로딩 이미지와 텍스트
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)

        // 비밀번호 입력 필드에 Enter 키 리스너 추가
        inputPw.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loginUser(inputId.text.toString().trim(), inputPw.text.toString().trim())
                true
            } else {
                false
            }
        }

        // 로그인 버튼 클릭 시
        loginBtn.setOnClickListener {
            loginUser(inputId.text.toString().trim(), inputPw.text.toString().trim())
        }

//        // Google 로그인 버튼 클릭 시
//        googleSignInButton.setOnClickListener {
//            showLoading() //로딩 출력
//            signIn()
//        }

        // 아이디 찾기 페이지로 이동
        findIdPage.setOnClickListener {
            val intent = Intent(this, FindIdActivity::class.java)
            startActivity(intent)
        }

        // 비밀번호 찾기 페이지로 이동
        findPwPage.setOnClickListener {
            val intent = Intent(this, FindPwActivity::class.java)
            startActivity(intent)
        }

        // 회원가입 페이지로 이동
        registerPage.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 알림 권한 확인 및 요청
        if (!checkNotificationPermission()) {
            requestNotificationPermission()
        }
    }

    private fun loginUser(userId: String, password: String) {
        if (userId.isNotEmpty() && password.isNotEmpty()) {
            showLoading() //로딩 이미지와 텍스트 표시

            // 사용자 아이디로 이메일 찾기
            val userRef = FirebaseDatabase.getInstance().reference.child("users")
            userRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            if (email != null) {
                                signInWithEmail(email, password)
                            } else {
                                hideLoading() //로딩 없애기 처리
                                Toast.makeText(this@LoginActivity, "No email corresponding to the ID was found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        hideLoading() //로딩 없애기 처리
                        Toast.makeText(this@LoginActivity, "This ID is not registered.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    hideLoading() //로딩 없애기 처리
                    Toast.makeText(this@LoginActivity, "Database ERROR: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            hideLoading() //로딩 없애기 처리
            Toast.makeText(this, "Please enter your ID and password.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            hideLoading() //로딩 없애기 처리
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid
                    if (userId != null) {
                        checkUserExistsAndLogin(userId)
                    }
                } else {
                    hideLoading() //로딩 없애기 처리
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        checkUserExistsAndLogin(userId)
                    } else {
                        hideLoading() //로딩 없애기 처리
                        Toast.makeText(this, "Failed to login.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideLoading() //로딩 없애기 처리
                    //이렇게 하면 이상하에 gmail 로그인 시에도 Wrong ID OR PW 경고가 나타남
//                    val errorMessage = when (task.exception) {
//                        is FirebaseAuthInvalidUserException -> "This account is not registered"
//                        is FirebaseAuthInvalidCredentialsException -> "Wrong ID or Wrong PW"
//                        else -> "Failed to login."
//                    }
                    // 여기서 구글 로그인으로 로그인된 경우 토스트 메시지를 표시하지 않도록 처리
                    if (task.exception !is FirebaseAuthInvalidUserException && task.exception !is FirebaseAuthInvalidCredentialsException) {
                        val errorMessage = "Failed to login."
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun checkUserExistsAndLogin(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val intent = Intent(this, Main_UI::class.java)
                startActivity(intent)
                finish()
            } else {
                val user = auth.currentUser
                val email = user?.email
                if (email != null) {
                    addUserToDatabase(userId, email)
                }
            }
        }.addOnFailureListener { exception ->
            hideLoading() //로딩 없애기 처리
            Toast.makeText(this, "Database access failed. Login failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addUserToDatabase(userId: String, email: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        val userData = hashMapOf(
            "email" to email,
            "userId" to userId
        )
        userRef.setValue(userData)
            .addOnSuccessListener {
                val intent = Intent(this, Main_UI::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                hideLoading() //로딩 없애기 처리
                Toast.makeText(this, "Failed to add user information to database: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        loadingImage.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        loadingImage.startAnimation(rotateAnimation)
        startLoadingTextAnimation()
    }

    private fun hideLoading() {
        loadingImage.visibility = View.GONE
        loadingText.visibility = View.GONE
        loadingImage.clearAnimation()
        handler.removeCallbacksAndMessages(null) // 애니메이션 중지
    }

    private fun startLoadingTextAnimation() {
        var dotCount = 0
        handler.post(object : Runnable {
            override fun run() {
                dotCount++
                if (dotCount > 3) {
                    dotCount = 0
                }
                val dots = ".".repeat(dotCount)
                loadingText.text = "loading$dots"
                handler.postDelayed(this, 500) // 500ms마다 업데이트
            }
        })
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Notification permission granted")
                } else {
                    Log.d(TAG, "Notification permission denied")
                    Toast.makeText(this, "Notification permission is required for proper app functionality.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}
