package com.studiomath.pencilnotes.ui.composeComponents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.file.FileExplorerViewModel

@Composable
fun HomeComponent(
    modifier: Modifier = Modifier,
    fileExplorerViewModel: FileExplorerViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.button_recents),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fileExplorerViewModel.recentFiles) { file ->
                ListItem(
                    dataFile = file,
                    fileExplorerViewModel = fileExplorerViewModel
                )
            }
        }
    }
}
