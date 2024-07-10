package com.example.testfolder

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class GameRecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_record)

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        SingletonKotlin.loadGameResults { results ->
            val htmlContent = generateHtmlContent(results)
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }

    private fun generateHtmlContent(results: List<Map<String, Any>>): String {
        val gameTypeGroups = results.groupBy { it["gameType"] as String }

        val charts = gameTypeGroups.map { (gameType, gameResults) ->
            val correctAnswersData = gameResults.groupBy { it["timestamp"] as Long }.map { (timestamp, dailyResults) ->
                val correctAnswers = dailyResults.sumOf { it["correctAnswers"] as Long }
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                "['$date', $correctAnswers]"
            }.joinToString(",\n")

            val totalTimeData = gameResults.groupBy { it["timestamp"] as Long }.map { (timestamp, dailyResults) ->
                val totalTime = dailyResults.sumOf { it["totalTime"] as Long }
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                "['$date', ${totalTime / 1000}]"
            }.joinToString(",\n")

            """
                <h3>$gameType - Correct Answers</h3>
                <div id="correct_answers_chart_$gameType" style="width: 100%; height: 400px;"></div>
                <h3>$gameType - Total Time</h3>
                <div id="total_time_chart_$gameType" style="width: 100%; height: 400px;"></div>
                <script type="text/javascript">
                    google.charts.setOnLoadCallback(function() {
                        var correctAnswersData = google.visualization.arrayToDataTable([
                            ['Date', 'Correct Answers'],
                            $correctAnswersData
                        ]);

                        var totalTimeData = google.visualization.arrayToDataTable([
                            ['Date', 'Total Time (seconds)'],
                            $totalTimeData
                        ]);

                        var correctAnswersOptions = {
                            title: '$gameType - Correct Answers by Date',
                            hAxis: { title: 'Date', titleTextStyle: { color: '#333' } },
                            vAxis: { minValue: 0 },
                            curveType: 'function',
                            legend: { position: 'bottom' }
                        };

                        var totalTimeOptions = {
                            title: '$gameType - Total Time by Date',
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
}
