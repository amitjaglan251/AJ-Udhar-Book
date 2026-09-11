package com.aj.udharbook.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aj.udharbook.model.Customer
import com.aj.udharbook.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerScreen(
    customer: Customer,
    viewModel: CustomerViewModel,
    onSaved: () -> Unit
) {

    var name by remember {
        mutableStateOf(customer.name)
    }

    var mobile by remember {
        mutableStateOf(customer.mobile)
    }

    var address by remember {
        mutableStateOf(customer.address)
    }

    Scaffold(

        topBar = {

            TopAppBar(
                title = {
                    Text("Edit Customer")
                }
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // CUSTOMER NAME
            // ==========================================

            OutlinedTextField(

                value = name,

                onValueChange = {
                    name = it
                },

                label = {
                    Text("Customer Name")
                },

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ==========================================
            // MOBILE
            // ==========================================

            OutlinedTextField(

                value = mobile,

                onValueChange = {
                    mobile = it
                },

                label = {
                    Text("Mobile Number")
                },

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ==========================================
            // ADDRESS
            // ==========================================

            OutlinedTextField(

                value = address,

                onValueChange = {
                    address = it
                },

                label = {
                    Text("Address")
                },

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ==========================================
            // SAVE CHANGES
            // ==========================================

            Button(

                onClick = {

                    if (
                        name.isNotBlank() &&
                        mobile.isNotBlank()
                    ) {

                        viewModel.update(

                            Customer(

                                id = customer.id,

                                name = name,

                                mobile = mobile,

                                address = address
                            )
                        )

                        onSaved()
                    }
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Save Changes")
            }
        }
    }
}