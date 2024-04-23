package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var localUser: String
    lateinit var loader: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()
        getAllReposByUserName(localUser)
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        loader = findViewById(R.id.pb_loader)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            if (validateFields()) {
                getAllReposByUserName(nomeUsuario.text.toString())
            }
        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal(username: String) {
        val sharePref =getPreferences(Context.MODE_PRIVATE)

        with(sharePref.edit()){
            putString("github_username", username)
            apply()
        }
    }

    private fun getUserLocal(): String{
        val sharePref =getPreferences(Context.MODE_PRIVATE)

        return sharePref.getString("github_username", null) ?: "null"
    }

    private fun showUserName() {
        localUser = getUserLocal()

        if (localUser != "null"){
            nomeUsuario.setText(localUser)
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        val retrofit =Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName(username: String) {
        loader.isVisible = true
        listaRepositories.isVisible = false
        if (localUser != "null" || nomeUsuario.text.isNotEmpty()){
            githubApi.getAllRepositoriesByUser(username).enqueue(object : Callback<List<Repository>>{
                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    if (response.isSuccessful) {
                        saveUserLocal(nomeUsuario.text.toString())
                        loader.isVisible = false
                        listaRepositories.isVisible = true

                        response.body()?.let {
                            setupAdapter(it)
                        }
                    }else{
                        loader.isVisible = false
                        Toast.makeText(baseContext, "Usuário não encontrado", Toast.LENGTH_LONG).show()
                    }
                }


                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(baseContext, "Algo deu errado tente novamente mais tarde" + t.message, Toast.LENGTH_LONG).show()
                }
            })
        }else{
            loader.isVisible = false
        }
    }

    private fun validateFields(): Boolean{
        if (nomeUsuario.text.trim().isEmpty()) false

        return true
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {

        val repositoryAdapter = RepositoryAdapter(list)
        listaRepositories.adapter = repositoryAdapter

        repositoryAdapter.repositoryItemLister ={ repository ->
            openBrowser(repository.htmlUrl)
        }
        repositoryAdapter.btnShareLister= { repository ->
            shareRepositoryLink(repository.htmlUrl)
        }
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio
    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

}