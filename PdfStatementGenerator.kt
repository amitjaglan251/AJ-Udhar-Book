package com.aj.udharbook.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.aj.udharbook.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementGenerator {

    fun generateStatement(
        context: Context,
        customerName: String,
        mobile: String,
        address: String,
        transactions: List<Transaction>
    ): File {

        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            1
        ).create()

        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }

        val headingPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        val normalPaint = Paint().apply {
            textSize = 14f
        }

        val boldPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }

        var y = 50f

        canvas.drawText(
            "AJ UDhar Book",
            40f,
            y,
            titlePaint
        )

        y += 30f

        canvas.drawText(
            "Customer Statement",
            40f,
            y,
            headingPaint
        )

        y += 35f

        canvas.drawText(
            "Customer: $customerName",
            40f,
            y,
            normalPaint
        )

        y += 22f

        canvas.drawText(
            "Mobile: $mobile",
            40f,
            y,
            normalPaint
        )

        y += 22f

        canvas.drawText(
            "Address: $address",
            40f,
            y,
            normalPaint
        )

        y += 35f

        val totalUdhar = transactions
            .filter {
                it.type.equals("UDHAR", ignoreCase = true) ||
                        it.type.equals("UDHAAR", ignoreCase = true)
            }
            .sumOf { it.amount }

        val totalPayment = transactions
            .filter {
                it.type.equals("PAYMENT", ignoreCase = true)
            }
            .sumOf { it.amount }

        val balance = totalUdhar - totalPayment

        canvas.drawText(
            "Total Udhaar: ₹%.2f".format(
                Locale.US,
                totalUdhar
            ),
            40f,
            y,
            boldPaint
        )

        y += 24f

        canvas.drawText(
            "Total Payment: ₹%.2f".format(
                Locale.US,
                totalPayment
            ),
            40f,
            y,
            boldPaint
        )

        y += 24f

        canvas.drawText(
            "Remaining Balance: ₹%.2f".format(
                Locale.US,
                balance
            ),
            40f,
            y,
            boldPaint
        )

        y += 35f

        canvas.drawText(
            "Transaction History",
            40f,
            y,
            headingPaint
        )

        y += 28f

        transactions.forEach { transaction ->

            if (y > 780f) {
                return@forEach
            }

            val type = if (
                transaction.type.equals(
                    "PAYMENT",
                    ignoreCase = true
                )
            ) {
                "Payment"
            } else {
                "Udhaar"
            }

            val amount = "₹%.2f".format(
                Locale.US,
                transaction.amount
            )

            val date = SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            ).format(
                Date(transaction.timestamp)
            )

            canvas.drawText(
                "$type: $amount",
                40f,
                y,
                normalPaint
            )

            y += 20f

            canvas.drawText(
                date,
                60f,
                y,
                normalPaint
            )

            y += 20f

            if (transaction.note.isNotBlank()) {

                canvas.drawText(
                    "Note: ${transaction.note}",
                    60f,
                    y,
                    normalPaint
                )

                y += 20f
            }

            y += 8f
        }

        y = 810f

        canvas.drawText(
            "AJ DIGITAL POINT",
            40f,
            y,
            boldPaint
        )

        pdfDocument.finishPage(page)

        val fileName =
            "statement_${customerName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"

        val file = File(
            context.cacheDir,
            fileName
        )

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        return file
    }
}