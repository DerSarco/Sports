package com.cursosandroidant.sports

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cursosandroidant.sports.databinding.ActivityMainBinding
import com.cursosandroidant.sports.retrofit.WeatherEntity
import com.cursosandroidant.sports.retrofit.WeatherService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Dispatcher
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: SportListAdapter
    private lateinit var adapter: SportAdapter
    private lateinit var fibScope: Job

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.e("FIbError", "$throwable in $coroutineContext")

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextField()

        setupRecyclerView()
        setupActionBar()

        getAllSports()
    }


    private fun setupRecyclerView() {
        listAdapter = SportListAdapter(this)
        adapter = SportAdapter(this)
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
//            adapter = listAdapter
            adapter = this@MainActivity.adapter
        }
    }

    private fun sports(): MutableList<Sport> {
        val soccerSport = Sport(
            1,
            "Fútbol Soccer",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Football_%28Soccer%29.JPG/1024px-Football_%28Soccer%29.JPG"
        )
        val baseballSport = Sport(
            2,
            "Baseball",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f2/A_worn-out_baseball.JPG/2470px-A_worn-out_baseball.JPG"
        )
        val volleyballSport = Sport(
            3,
            "Volleyball",
            "https://upload.wikimedia.org/wikipedia/commons/b/bf/Algeria_and_Japan_women%27s_national_volleyball_team_at_the_2012_Summer_Olympics_%287913959028%29.jpg"
        )
        val boxingSport = Sport(
            4,
            "Boxeo",
            "https://upload.wikimedia.org/wikipedia/commons/4/4d/Boxing_at_the_2018_Summer_Youth_Olympics_%E2%80%93_Girls%27_flyweight_Bronze_Medal_Bout_068.jpg"
        )
        val tennisSport = Sport(
            5,
            "Tenis",
            "https://upload.wikimedia.org/wikipedia/commons/3/3e/Tennis_Racket_and_Balls.jpg"
        )
        val rugbySport = Sport(
            6,
            "Rugby",
            "https://upload.wikimedia.org/wikipedia/commons/6/6a/New_Zealand_national_rugby_20191101d4.jpg"
        )
        val hokeySport = Sport(
            7,
            "Hokey",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/2020-01-21_Ice_hockey_at_the_2020_Winter_Youth_Olympics_%E2%80%93_Women%27s_tournament_%E2%80%93_Gold_Medal_Game_%28Martin_Rulsch%29_068.jpg/2560px-2020-01-21_Ice_hockey_at_the_2020_Winter_Youth_Olympics_%E2%80%93_Women%27s_tournament_%E2%80%93_Gold_Medal_Game_%28Martin_Rulsch%29_068.jpg"
        )
        val golfSport =
            Sport(8, "Golf", "https://upload.wikimedia.org/wikipedia/commons/d/d9/Golf_ball_2.jpg")
        val chessSport =
            Sport(9, "Chess", "https://upload.wikimedia.org/wikipedia/commons/8/8c/Chess_Large.JPG")
        return mutableListOf(
            soccerSport, baseballSport, volleyballSport, boxingSport, tennisSport,
            rugbySport, hokeySport, golfSport, chessSport
        )
    }

    private fun getSportsFlow(): Flow<Sport> = flow {
        sports().forEach {
            delay(1_000)
            emit(it)
        }
    }.flowOn(Dispatchers.Default)

    private fun getAllSports() {
//        val sportsData = sports()
//        listAdapter.submitList(sportsData)
        lifecycleScope.launch {
            getSportsFlow().collect {
                adapter.add(it)
            }
//            sportsData.forEach { sport ->
//                adapter.add(sport)
//            }
        }
    }

    /**
     * Retrofit
     * */
    private fun setupActionBar() {
        lifecycleScope.launch {
            formatResponse(getWeather())
        }
    }

    private suspend fun getWeather(): WeatherEntity = withContext(Dispatchers.IO) {
        setupTitle(getString(R.string.main_retrofit_in_progress))

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service: WeatherService = retrofit.create(WeatherService::class.java)

        service.getWeatherById(3868121L, "metric", "")
    }

    private fun formatResponse(weatherEntity: WeatherEntity) {
        val temp = "${weatherEntity.main.temp} Cº"
        val name = weatherEntity.name
        val country = weatherEntity.sys.country
        setupTitle("$temp in $name, $country")
    }

    private fun setupTitle(newTitle: String) {
        supportActionBar?.let { title = newTitle }
    }

    private fun setupTextField() {


        binding.etNumber.addTextChangedListener {
            if (this::fibScope.isInitialized && fibScope.isActive) {
                fibScope.cancel()
            }
            binding.tvResult.text = "Calculando..."
            val time = System.currentTimeMillis()
            fibScope = CoroutineScope(Job() + exceptionHandler).launch(Dispatchers.Default) {
                if (it.toString().isNotEmpty()) {
                    val fib = fibonnaci(it.toString().toLong())
                    withContext(Dispatchers.Main) {
                        binding.tvResult.text =
                            "R= ${"%,d".format(fib)}\n(in ${System.currentTimeMillis() - time})"
                    }
                }
            }
        }
    }

    private fun fibonnaci(n: Long): Long {
        if (fibScope.isCancelled) {
            throw Exception("Número modificado antes de completar el cálculo")
        }


        return if (n <= 1) n
        else fibonnaci(n - 1) + fibonnaci(n - 2)
    }

    override fun onDestroy() {
        if (fibScope.isActive) fibScope.cancel()
        super.onDestroy()
    }

    /**
     * OnClickListener
     * */
    override fun onClick(sport: Sport) {
        Snackbar.make(binding.root, sport.name, Snackbar.LENGTH_SHORT).show()
    }
}
