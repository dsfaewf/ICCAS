package com.example.testfolder

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class OXGameRecordActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var currentViewMode = "daily"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oxgame_record)

        webView = findViewById(R.id.webView)
        val dailyButton = findViewById<Button>(R.id.dailyButton)
        val monthlyButton = findViewById<Button>(R.id.monthlyButton)
        val oxGameRecordButton = findViewById<Button>(R.id.oxGameRecordButton)
        val fourChoiceGameRecordButton = findViewById<Button>(R.id.fourChoiceGameRecordButton)
        val shortAnswerGameRecordButton = findViewById<Button>(R.id.shortAnswerGameRecordButton)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        dailyButton.setBackgroundResource(R.drawable.btn_rounded3)
        oxGameRecordButton.setBackgroundResource(R.drawable.btn_rounded2)

        SingletonKotlin.loadGameResults { results ->
            renderGraph(results.filter { it["gameType"] == "OX" })
        }

        dailyButton.setOnClickListener {
            currentViewMode = "daily"
            SingletonKotlin.loadGameResults { results ->
                renderGraph(results.filter { it["gameType"] == "OX" })
            }
            monthlyButton.setBackgroundResource(R.drawable.btn_rounded)
            dailyButton.setBackgroundResource(R.drawable.btn_rounded3)
        }

        monthlyButton.setOnClickListener {
            currentViewMode = "monthly"
            SingletonKotlin.loadGameResults { results ->
                renderGraph(results.filter { it["gameType"] == "OX" })
            }
            monthlyButton.setBackgroundResource(R.drawable.btn_rounded3)
            dailyButton.setBackgroundResource(R.drawable.btn_rounded)
        }
        oxGameRecordButton.setOnClickListener {
            val intent = Intent(this, OXGameRecordActivity::class.java)
            startActivity(intent)
            finish()
            oxGameRecordButton.setBackgroundResource(R.drawable.btn_rounded2)
            fourChoiceGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
            shortAnswerGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
        }

        fourChoiceGameRecordButton.setOnClickListener {
            val intent = Intent(this, FourChoiceGameRecordActivity::class.java)
            startActivity(intent)
            finish()
            oxGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
            fourChoiceGameRecordButton.setBackgroundResource(R.drawable.btn_rounded2)
            shortAnswerGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
        }

        shortAnswerGameRecordButton.setOnClickListener {
            val intent = Intent(this, ShortAnswerGameRecordActivity::class.java)
            startActivity(intent)
            finish()
            oxGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
            fourChoiceGameRecordButton.setBackgroundResource(R.drawable.btn_rounded)
            shortAnswerGameRecordButton.setBackgroundResource(R.drawable.btn_rounded2)
        }
    }

    private fun renderGraph(results: List<Map<String, Any>>) {
        val htmlContent = generateHtmlContent(results)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateHtmlContent(results: List<Map<String, Any>>): String {
        val dateFormat = if (currentViewMode == "daily") "dd-MM-yyyy" else "MM-yyyy"
        val dailyResults = results.groupBy {
            SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(it["timestamp"] as Long))
        }

        val correctAnswersData = dailyResults.map { (date, dailyGames) ->
            val totalCorrectAnswers = dailyGames.sumOf { (it["correctAnswers"] as Long).toInt() }
            val avgCorrectAnswers = totalCorrectAnswers.toFloat() / dailyGames.size
            "['$date', $avgCorrectAnswers]"
        }.joinToString(",\n")

        val totalTimeData = dailyResults.map { (date, dailyGames) ->
            val totalTime = dailyGames.sumOf { (it["totalTime"] as Long).toInt() }
            val avgTotalTime = totalTime.toFloat() / dailyGames.size
            "['$date', $avgTotalTime]"
        }.joinToString(",\n")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
                <script type="text/javascript">
                    google.charts.load('current', {'packages':['corechart']});
                    google.charts.setOnLoadCallback(drawCharts);

                    function drawCharts() {
                        var correctAnswersData = google.visualization.arrayToDataTable([
                            ['Date', 'Average Correct Answers'],
                            $correctAnswersData
                        ]);

                        var totalTimeData = google.visualization.arrayToDataTable([
                            ['Date', 'Average Total Time (seconds)'],
                            $totalTimeData
                        ]);

                        var correctAnswersOptions = {
                            title: 'Average Correct Answers by Date',
                            hAxis: { title: 'Date', titleTextStyle: { color: '#333' } },
                            vAxis: { minValue: 0, maxValue: 10 },
                            curveType: 'function',
                            legend: { position: 'bottom' }
                        };

                        var totalTimeOptions = {
                            title: 'Average Total Time by Date',
                            hAxis: { title: 'Date', titleTextStyle: { color: '#333' } },
                            vAxis: { minValue: 0, maxValue: 300 },
                            curveType: 'function',
                            legend: { position: 'bottom' }
                        };

                        var correctAnswersChart = new google.visualization.LineChart(document.getElementById('correct_answers_chart'));
                        correctAnswersChart.draw(correctAnswersData, correctAnswersOptions);

                        var totalTimeChart = new google.visualization.LineChart(document.getElementById('total_time_chart'));
                        totalTimeChart.draw(totalTimeData, totalTimeOptions);
                    }
                </script>
                <style>
                    h3 {
                        margin-left: 50px;
                    }
                </style>
            </head>
            <body>
                <h3> Correct Answers</h3>
                <div id="correct_answers_chart" style="width: 100%; height: 400px;"></div>
                <hr style="margin: 30px;">
                <h3 style="margin-right: 50px;"> Total time for the problem you got it right</h3>
                <div id="total_time_chart" style="width: 100%; height: 400px;"></div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun navigateToMain() {
        val intent = Intent(this, Main_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMain()
//        val intent = Intent(applicationContext, GamelistActivity::class.java)
//        startActivity(intent)
//        finish()
    }
}
