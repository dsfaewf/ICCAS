package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
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
            // 같은 아이디로만 로그인 되는 현상 해결을 위해 추가함.
        }

        val loginBtn = findViewById<Button>(R.id.login_btn)
        val findIdPage = findViewById<TextView>(R.id.find_id_tv)
        val findPwPage = findViewById<TextView>(R.id.find_pw_tv)
        val registerPage = findViewById<TextView>(R.id.register_tv)
        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.btn_google_sign_in)

        // 로그인 버튼 클릭 시
        loginBtn.setOnClickListener {
            val userId = findViewById<TextView>(R.id.input_id).text.toString()
            val password = findViewById<TextView>(R.id.input_pw).text.toString()

            if (userId.isNotEmpty() && password.isNotEmpty()) {
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
                                    Toast.makeText(this@LoginActivity, "아이디에 해당하는 이메일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "등록되지 않은 아이디입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@LoginActivity, "데이터베이스 오류: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // Google 로그인 버튼 클릭 시
        googleSignInButton.setOnClickListener {
            signIn()
        }

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
                        Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "등록되지 않은 계정입니다."
                        is FirebaseAuthInvalidCredentialsException -> "잘못된 아이디 또는 비밀번호입니다."
                        else -> "로그인에 실패했습니다."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 사용자가 존재하는지 확인하고 로그인 처리
    private fun checkUserExistsAndLogin(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 회원가입을 마친 사용자인 경우 로그인 성공
                val intent = Intent(this, Main_UI::class.java)
                startActivity(intent)
                finish()
            } else {
                // 회원가입을 마치지 않은 사용자인 경우 사용자 정보 추가
                val user = auth.currentUser
                val email = user?.email
                if (email != null) {
                    addUserToDatabase(userId, email)
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "데이터베이스 접근 실패. 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addUserToDatabase(userId: String, email: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        val userData = hashMapOf(
            "email" to email
            // 다른 사용자 정보도 추가할 수 있음
        )
        userRef.setValue(userData)
            .addOnSuccessListener {
                // 데이터베이스에 사용자 정보 추가 성공
                val intent = Intent(this, Main_UI::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                // 데이터베이스에 사용자 정보 추가 실패
                Toast.makeText(this, "데이터베이스에 사용자 정보 추가 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
