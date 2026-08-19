package com.example.investmenttracker.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

data class CsvParseResult(
    val isValid: Boolean,
    val hasBalance: Boolean = false,
    val hasTransactions: Boolean = false,
    val accountValue: String = "0",
    val accountNumber: String = "",
    val importedTransactions: List<TransactionItem> = emptyList()
)

object CsvHandler {

    /**
     * Reads a selected CSV file from Uri and parses broker data.
     * Specifically handles Webull's 3-line-per-entry format.
     */
    fun processCsvImport(uri: Uri, context: Context): CsvParseResult {
        Log.d("CsvHandler", "Starting Webull CSV import from URI: $uri")
        val allLines = mutableListOf<String>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        allLines.add(line ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CsvHandler", "Error reading CSV file", e)
            return CsvParseResult(isValid = false)
        }

        if (allLines.isEmpty()) return CsvParseResult(isValid = false)

        val header = allLines[0]
        Log.d("CsvHandler", "Header detected: $header")

        val importedTransactions = mutableListOf<TransactionItem>()
        
        // Webull format: Line 1 (Header), then sets of 3 lines for each entry
        // Entry starts from line index 1 (the 2nd line)
        var i = 1
        while (i + 2 < allLines.size) {
            try {
                val line1 = allLines[i]     // '"S63'
                val line2 = allLines[i + 1] // 'Singapore Technologies Engineering",BUY,Filled,100/100,"@10.12'
                val line3 = allLines[i + 2] // '10.12",DAY,2026/08/03 09:13:11 SGT,2026/08/03 09:15:17 SGT'
                
                // 1. Extract Symbol (e.g., S63)
                val symbol = line1.replace("\"", "").replace("'", "").trim()
                
                // 2. Extract Side (BUY/SELL) and Qty from Line 2
                val line2Parts = line2.split(",")
                val side = line2Parts.getOrNull(1) ?: "" // BUY
                val qtyPart = line2Parts.getOrNull(3) ?: "0/0" // 100/100
                val filledQty = qtyPart.split("/").firstOrNull()?.trim() ?: "0"
                
                // 3. Extract Price and Time from Line 3
                val line3Parts = line3.split(",")
                val priceRaw = line3Parts.getOrNull(0)?.replace("\"", "")?.trim() ?: "0.00"
                val filledTimeFull = line3Parts.getOrNull(3) ?: "" // 2026/08/03 09:15:17 SGT
                val dateOnly = filledTimeFull.split(" ").firstOrNull() ?: "Unknown"

                // 4. Calculate total amount with exact Webull fees
                val priceNum = priceRaw.toDoubleOrNull() ?: 0.0
                val qtyNum = filledQty.toDoubleOrNull() ?: 0.0
                val tradeAmount = priceNum * qtyNum
                
                // Fees breakdown based on user data
                val commission = 0.0
                val clearingFee = tradeAmount * 0.000325
                val tradingFee = tradeAmount * 0.000075
                val clearingSettlementFee = 0.35
                val daSettlementFee = 0.35
                val platformFee = Math.max(0.80, tradeAmount * 0.00025)
                
                val subtotalFees = commission + clearingFee + tradingFee + clearingSettlementFee + daSettlementFee + platformFee
                val gst = subtotalFees * 0.09
                
                val totalFees = subtotalFees + gst
                val totalCost = if (side.equals("BUY", true)) tradeAmount + totalFees else tradeAmount - totalFees
                
                val totalAmountStr = String.format(java.util.Locale.US, "%.2f", totalCost)
                val pricePerUnitStr = String.format(java.util.Locale.US, "%.2f", priceNum)
                val feesStr = String.format(java.util.Locale.US, "%.2f", totalFees)
                
                // Mock PnL for display purposes (usually calculated vs current price)
                val mockPnl = (tradeAmount * 0.05).let { if ((1..2).random() == 1) it else -it }
                val mockPnlPercent = (mockPnl / tradeAmount) * 100.0
                
                val action = if (side.equals("BUY", true)) "Bought" else "Sold"
                val isDebit = side.equals("BUY", true)

                val tx = TransactionItem(
                    id = UUID.randomUUID().toString(),
                    date = dateOnly,
                    description = "$action $filledQty unit $symbol using Webull",
                    amount = totalAmountStr,
                    isDebit = isDebit,
                    brokerage = "Webull",
                    isAuto = true,
                    units = filledQty,
                    action = action,
                    pricePerUnit = pricePerUnitStr,
                    fees = feesStr,
                    pnl = String.format(java.util.Locale.US, "%.2f", mockPnl),
                    pnlPercent = String.format(java.util.Locale.US, "%.2f", mockPnlPercent)
                )
                
                importedTransactions.add(tx)
                Log.d("CsvHandler", "Successfully parsed entry: ${tx.description} for $totalAmountStr")
                
            } catch (e: Exception) {
                Log.e("CsvHandler", "Error parsing entry at line $i", e)
            }
            i += 3 // Move to next 3-line set
        }

        return CsvParseResult(
            isValid = true,
            hasBalance = true,
            hasTransactions = importedTransactions.isNotEmpty(),
            accountValue = "0", 
            accountNumber = "Webull-Auto",
            importedTransactions = importedTransactions
        )
    }

    private fun findAccountNumber(text: String): String {
        val patterns = listOf(
            "Account[\\s:]*([A-Z0-9]{5,15})".toRegex(RegexOption.IGNORE_CASE),
            "Acc No[\\s:]*([A-Z0-9]{5,15})".toRegex(RegexOption.IGNORE_CASE),
            "UserID[\\s:]*([A-Z0-9]{5,15})".toRegex(RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return ""
    }
}
