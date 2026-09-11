package com.aj.udharbook.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.aj.udharbook.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    /**
     * Customer Details PDF
     */
    fun generateCustomerPdf(
        context: Context,
        customerName: String,
        mobile: String,
        address: String,
        transactions: List<Transaction>
    ): File {

        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint().apply {
            isAntiAlias = true
        }

        var pageNumber = 1
        var y = 50

        fun startPage(): PdfDocument.Page {
            return pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber++
                ).create()
            )
        }

        var page = startPage()
        var canvas = page.canvas

        // --------------------------------------------------
        // TITLE
        // --------------------------------------------------

        paint.textSize = 24f
        paint.isFakeBoldText = true

        canvas.drawText(
            "AJ Udhar Book",
            40f,
            y.toFloat(),
            paint
        )

        y += 35

        paint.textSize = 18f

        canvas.drawText(
            "Customer Statement",
            40f,
            y.toFloat(),
            paint
        )

        y += 35

        // --------------------------------------------------
        // CUSTOMER DETAILS
        // --------------------------------------------------

        paint.textSize = 14f
        paint.isFakeBoldText = true

        canvas.drawText(
            "Customer: $customerName",
            40f,
            y.toFloat(),
            paint
        )

        y += 22

        paint.isFakeBoldText = false

        if (mobile.isNotBlank()) {
            canvas.drawText(
                "Mobile: $mobile",
                40f,
                y.toFloat(),
                paint
            )

            y += 22
        }

        if (address.isNotBlank()) {
            canvas.drawText(
                "Address: $address",
                40f,
                y.toFloat(),
                paint
            )

            y += 30
        }

        // --------------------------------------------------
        // TOTALS
        // --------------------------------------------------

        val totalUdhar =
            transactions
                .filter {
                    it.type.equals(
                        "UDHAR",
                        ignoreCase = true
                    )
                }
                .sumOf {
                    it.amount
                }

        val totalPayment =
            transactions
                .filter {
                    it.type.equals(
                        "PAYMENT",
                        ignoreCase = true
                    )
                }
                .sumOf {
                    it.amount
                }

        val balance =
            totalUdhar - totalPayment

        paint.isFakeBoldText = true

        canvas.drawText(
            "Total Udhar: ${formatAmount(totalUdhar)}",
            40f,
            y.toFloat(),
            paint
        )

        y += 22

        canvas.drawText(
            "Total Payment: ${formatAmount(totalPayment)}",
            40f,
            y.toFloat(),
            paint
        )

        y += 22

        canvas.drawText(
            "Current Balance: ${formatAmount(balance)}",
            40f,
            y.toFloat(),
            paint
        )

        y += 35

        // --------------------------------------------------
        // TRANSACTION HEADER
        // --------------------------------------------------

        paint.textSize = 15f

        canvas.drawText(
            "Transaction History",
            40f,
            y.toFloat(),
            paint
        )

        y += 25

        paint.textSize = 11f
        paint.isFakeBoldText = true

        canvas.drawText(
            "Date",
            40f,
            y.toFloat(),
            paint
        )

        canvas.drawText(
            "Type",
            180f,
            y.toFloat(),
            paint
        )

        canvas.drawText(
            "Amount",
            300f,
            y.toFloat(),
            paint
        )

        canvas.drawText(
            "Note",
            400f,
            y.toFloat(),
            paint
        )

        y += 20

        paint.isFakeBoldText = false

        // --------------------------------------------------
        // TRANSACTIONS
        // --------------------------------------------------

        for (transaction in transactions) {

            if (y > pageHeight - 60) {

                pdfDocument.finishPage(page)

                page = startPage()
                canvas = page.canvas

                y = 50

                paint.textSize = 11f

                canvas.drawText(
                    "Transaction History - Continued",
                    40f,
                    y.toFloat(),
                    paint
                )

                y += 25
            }

            val date =
                formatDate(transaction.timestamp)

            val type =
                if (
                    transaction.type.equals(
                        "UDHAR",
                        ignoreCase = true
                    )
                ) {
                    "Udhar"
                } else {
                    "Payment"
                }

            val note =
                transaction.note
                    .replace("\n", " ")
                    .take(22)

            canvas.drawText(
                date.take(18),
                40f,
                y.toFloat(),
                paint
            )

            canvas.drawText(
                type,
                180f,
                y.toFloat(),
                paint
            )

            canvas.drawText(
                formatAmount(transaction.amount),
                300f,
                y.toFloat(),
                paint
            )

            canvas.drawText(
                note,
                400f,
                y.toFloat(),
                paint
            )

            y += 20
        }

        // --------------------------------------------------
        // FOOTER
        // --------------------------------------------------

        if (y > pageHeight - 40) {

            pdfDocument.finishPage(page)

            page = startPage()
            canvas = page.canvas

            y = pageHeight - 30

        } else {

            y = pageHeight - 30
        }

        paint.textSize = 9f

        canvas.drawText(
            "Generated by AJ Udhar Book",
            40f,
            y.toFloat(),
            paint
        )

        canvas.drawText(
            "Date: ${formatDate(System.currentTimeMillis())}",
            350f,
            y.toFloat(),
            paint
        )

        // --------------------------------------------------
        // FINISH PAGE
        // --------------------------------------------------

        pdfDocument.finishPage(page)

        // --------------------------------------------------
        // SAVE FILE
        // --------------------------------------------------

        val directory =
            File(
                context.cacheDir,
                "pdf"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file =
            File(
                directory,
                "AJ_UdharBook_${customerName.replace(" ", "_")}.pdf"
            )

        FileOutputStream(file).use {
            pdfDocument.writeTo(it)
        }

        pdfDocument.close()

        return file
    }


    // ======================================================
    // FORMAT AMOUNT
    // ======================================================

    private fun formatAmount(
        amount: Double
    ): String {

        return "₹%.2f".format(
            Locale.US,
            amount
        )
    }


    // ======================================================
    // FORMAT DATE
    // ======================================================

    private fun formatDate(
        timestamp: Long
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

        return formatter.format(
            Date(timestamp)
        )
    }
}
