package com.ahmedonibiyo.websocketapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedonibiyo.websocketapp.model.ConnectionState
import com.ahmedonibiyo.websocketapp.ui.theme.ConnectedGreen
import com.ahmedonibiyo.websocketapp.ui.theme.ConnectingOrange
import com.ahmedonibiyo.websocketapp.ui.theme.DisconnectedGray
import com.ahmedonibiyo.websocketapp.ui.theme.FailedRed
import com.ahmedonibiyo.websocketapp.ui.theme.PureBlack
import com.ahmedonibiyo.websocketapp.ui.theme.PureWhite

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    showButtons: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.CONNECTED -> ConnectedGreen
                ConnectionState.CONNECTING -> ConnectingOrange
                ConnectionState.FAILED -> FailedRed
                ConnectionState.DISCONNECTED -> DisconnectedGray
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status: ${connectionState.name}",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if(showButtons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConnect,
                        enabled = connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.CONNECTING,
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite)
                    ) {
                        Text("Connect", color = PureBlack)
                    }

                    Button(
                        onClick = onDisconnect,
                        enabled = connectionState == ConnectionState.CONNECTED,
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite)
                    ) {
                        Text("Disconnect", color = PureBlack)
                    }
                }
            }
        }
    }
}