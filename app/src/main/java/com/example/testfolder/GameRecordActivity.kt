package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class GameRecordActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var currentViewMode = "daily"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_record)

        webView = findViewById(R.id.webView)
        val dailyButton = findViewById<Button>(R.id.dailyButton)
        val monthlyButton = findViewById<Button>(R.id.monthlyButton)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        SingletonKotlin.loadGameResults { results ->
            renderGraph(results)
        }

        dailyButton.setOnClickListener {
            currentViewMode = "daily"
            SingletonKotlin.loadGameResults { results ->
                renderGraph(results)
            }
        }

        monthlyButton.setOnClickListener {
            currentViewMode = "monthly"
            SingletonKotlin.loadGameResults { results ->
                renderGraph(results)
            }
        }
    }

    private fun renderGraph(results: List<Map<String, Any>>) {
        val htmlContent = generateHtmlContent(results)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateHtmlContent(results: List<Map<String, Any>>): String {
        val gameTypeGroups = results.groupBy { it["gameType"] as String }

        val charts = gameTypeGroups.map { (gameType, gameResults) ->
            val dateFormat = if (currentViewMode == "daily") "dd-MM-yyyy" else "MM-yyyy"
            val dailyResults = gameResults.groupBy {
                SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(it["timestamp"] as Long))
            }

            val correctAnswersData = dailyResults.map { (date, dailyGames) ->
                val totalCorrectAnswers = dailyGames.sumOf { (it["correctAnswers"] as Long).toInt() }
                val avgCorrectAnswers = totalCorrectAnswers.toFloat() / dailyGames.size
                "['$date', $avgCorrectAnswers]"
            }.joinToString(",\n")

            val totalTimeData = dailyResults.map { (date, dailyGames) ->
                val totalTime = dailyGames.sumOf { (it["totalTime"] as Long).toInt() }
                val avgTotalTime = totalTime.toFloat() / dailyGames.size // 초 단위로 변환 불필요
                "['$date', $avgTotalTime]"
            }.joinToString(",\n")

            """
                <h3>$gameType - Correct Answers</h3>
                <div id="correct_answers_chart_$gameType" style="width: 100%; height: 400px;"></div>
                <h3>$gameType - Total Time</h3>
                <div id="total_time_chart_$gameType" style="width: 100%; height: 400px;"></div>
                <script type="text/javascript">
                    google.charts.setOnLoadCallback(function() {
                        var correctAnswersData = google.visualization.arrayToDataTable([
                            ['Date', 'Average Correct Answers'],
                            $correctAnswersData
                        ]);

                        var totalTimeData = google.visualization.arrayToDataTable([
                            ['Date', 'Average Total Time (seconds)'],
                            $totalTimeData
                        ]);

                        var correctAnswersOptions = {
                            title: '$gameType - Average Correct Answers by Date',
                            hAxis: { title: 'Date', titleTextStyle: { color: '#333' } },
                            vAxis: { minValue: 0 },
                            curveType: 'function',
                            legend: { position: 'bottom' }
                        };

                        var totalTimeOptions = {
                            title: '$gameType - Average Total Time by Date',
                            hAxis: { title: 'Date', titleTextStyle: { color: '#333' } },
                            vAxis: { minValue: 0 },
                            curveType: 'function',
                            legend: { position: 'bottom' }
                        };

                        var correctAnswersChart = new google.visualization.LineChart(document.getElementById('correct_answers_chart_$gameType'));
                        correctAnswersChart.draw(correctAnswersData, correctAnswersOptions);

                        var totalTimeChart = new google.visualization.LineChart(document.getElementById('total_time_chart_$gameType'));
                        totalTimeChart.draw(totalTimeData, totalTimeOptions);
                    });
                </script>
            """.trimIndent()
        }.joinToString("\n")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
                <script type="text/javascript">
                    google.charts.load('current', {'packages':['corechart']});
                </script>
            </head>
            <body>
                $charts
            </body>
            </html>
        """.trimIndent()
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, Main_UI::class.java)
        startActivity(intent)
        finish()
    }
}
