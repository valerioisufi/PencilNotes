package com.studiomath.pencilnotes.ui.composeComponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studiomath.pencilnotes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestNameDialog(
    title: String = "",
    labelTextField: String = "",
    textConfirmButton: String = "",
    onDismissRequest: () -> Unit,
    onConfirm: (inputText: String) -> Unit,
    isAllowedInput: (inputText: String) -> String = { "" }
){
    var validInputError by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            onDismissRequest()
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var text by remember { mutableStateOf("") }
                OutlinedTextField(value = text,
                    onValueChange = { text = it; validInputError = isAllowedInput(it) },
                    label = { Text(labelTextField) }
                )
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        onConfirm(text)
                    },
                    enabled = validInputError.isEmpty()
                ) {
                    Text(text = textConfirmButton)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmActionDialog(
    title: String = "",
    textDescription: String = "",
    textConfirmButton: String = stringResource(id = R.string.button_confirm),
    textCancelButton: String = stringResource(id = R.string.menu_cancel),
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
){

    BasicAlertDialog(
        onDismissRequest = {
            onDismissRequest()
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = textDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
                Row (
                    modifier = Modifier.align(Alignment.End),
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                        }
                    ) {
                        Text(text = textCancelButton)
                    }

                    TextButton(
                        onClick = {
                            onConfirm()
                        }
                    ) {
                    Text(text = textConfirmButton)
                }
                }

            }
        }
    }
}