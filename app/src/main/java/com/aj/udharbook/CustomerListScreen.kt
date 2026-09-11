package com.aj.udharbook.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aj.udharbook.navigation.Screen
import com.aj.udharbook.viewmodel.CustomerViewModel

// ==========================================================
// CUSTOMER LIST COLORS
// ==========================================================

private val HeaderBlue = Color(0xFF1976D2)
private val AddBlue = Color(0xFF1565C0)
private val SearchBlue = Color(0xFF42A5F5)

private val CardColors = listOf(
    Color(0xFFE3F2FD),
    Color(0xFFE8F5E9),
    Color(0xFFFFF3E0),
    Color(0xFFF3E5F5),
    Color(0xFFE0F7FA)
)

private val AccentColors = listOf(
    Color(0xFF1976D2),
    Color(0xFF2E7D32),
    Color(0xFFEF6C00),
    Color(0xFF7B1FA2),
    Color(0xFF00838F)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    navController: NavController,
    viewModel: CustomerViewModel
) {

    val customers by viewModel.allCustomers.collectAsState(
        initial = emptyList()
    )

    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredCustomers =
        customers.filter { customer ->

            customer.name.contains(
                searchQuery,
                ignoreCase = true
            ) ||
                    customer.mobile.contains(
                        searchQuery,
                        ignoreCase = true
                    )
        }

    Scaffold(

        // ==================================================
        // TOP BAR
        // ==================================================

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Customer List",

                            color = Color.White,

                            fontSize = 21.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "${customers.size} Customers",

                            color =
                                Color.White.copy(
                                    alpha = 0.85f
                                ),

                            fontSize = 13.sp
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            HeaderBlue
                    )
            )
        },

        // ==================================================
        // ADD CUSTOMER
        // ==================================================

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    navController.navigate(
                        "add_customer"
                    )
                },

                containerColor =
                    AddBlue,

                contentColor =
                    Color.White
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Add Customer"
                )
            }
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            // ==================================================
            // SEARCH AREA
            // ==================================================

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFE3F2FD)
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    ),

                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    Text(
                        text =
                            "🔍 Search Customer",

                        color =
                            HeaderBlue,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            16.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    OutlinedTextField(

                        value =
                            searchQuery,

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "Name or Mobile Number"
                            )
                        },

                        placeholder = {
                            Text(
                                "Search here..."
                            )
                        },

                        singleLine = true,

                        trailingIcon = {

                            if (
                                searchQuery.isNotEmpty()
                            ) {

                                IconButton(

                                    onClick = {
                                        searchQuery = ""
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Clear,

                                        contentDescription =
                                            "Clear Search"
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ==================================================
            // RESULT COUNT
            // ==================================================

            if (
                filteredCustomers.isNotEmpty()
            ) {

                Text(
                    text =
                        "Showing ${filteredCustomers.size} customer(s)",

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        ),

                    color =
                        Color.Gray,

                    fontSize =
                        13.sp
                )
            }

            // ==================================================
            // EMPTY STATE
            // ==================================================

            if (filteredCustomers.isEmpty()) {

                Box(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Card(

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFF5F5F5)
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 3.dp
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier.padding(28.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "👤",

                                fontSize = 48.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    if (
                                        searchQuery.isEmpty()
                                    ) {
                                        "No Customers Found"
                                    } else {
                                        "No matching customer found"
                                    },

                                fontSize = 18.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    if (
                                        searchQuery.isEmpty()
                                    ) {
                                        "Add a customer to get started."
                                    } else {
                                        "Try another name or mobile number."
                                    },

                                color =
                                    Color.Gray
                            )
                        }
                    }
                }

            } else {

                // ==================================================
                // CUSTOMER LIST
                // ==================================================

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 90.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(

                        items =
                            filteredCustomers,

                        key = { customer ->
                            customer.id
                        }

                    ) { customer ->

                        val index =
                            filteredCustomers.indexOf(
                                customer
                            )

                        val cardColor =
                            CardColors[
                                index % CardColors.size
                            ]

                        val accentColor =
                            AccentColors[
                                index % AccentColors.size
                            ]

                        // ==================================================
                        // CUSTOMER CARD
                        // ==================================================

                        Card(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        navController.navigate(

                                            Screen.CustomerDetails
                                                .createRoute(
                                                    customer.id
                                                )
                                        )
                                    },

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        cardColor
                                ),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 5.dp
                                ),

                            shape =
                                RoundedCornerShape(16.dp)
                        ) {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                // ==================================================
                                // CUSTOMER ICON
                                // ==================================================

                                Box(

                                    modifier =
                                        Modifier
                                            .size(52.dp)
                                            .background(
                                                color =
                                                    accentColor,
                                                shape =
                                                    CircleShape
                                            ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Person,

                                        contentDescription =
                                            "Customer",

                                        tint =
                                            Color.White,

                                        modifier =
                                            Modifier.size(
                                                30.dp
                                            )
                                    )
                                }

                                Spacer(
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 8.dp
                                        )
                                )

                                // ==================================================
                                // CUSTOMER DETAILS
                                // ==================================================

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text =
                                            customer.name,

                                        color =
                                            accentColor,

                                        fontSize =
                                            19.sp,

                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(4.dp)
                                    )

                                    Text(
                                        text =
                                            "📱 ${customer.mobile}",

                                        color =
                                            Color.DarkGray,

                                        fontSize =
                                            14.sp
                                    )

                                    if (
                                        customer.address
                                            .isNotBlank()
                                    ) {

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    3.dp
                                                )
                                        )

                                        Text(
                                            text =
                                                "📍 ${customer.address}",

                                            color =
                                                Color.DarkGray,

                                            fontSize =
                                                13.sp,

                                            maxLines = 2
                                        )
                                    }
                                }

                                // ==================================================
                                // ARROW
                                // ==================================================

                                Text(
                                    text = "›",

                                    color =
                                        accentColor,

                                    fontSize = 32.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
