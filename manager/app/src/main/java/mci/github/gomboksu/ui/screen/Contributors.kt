package mci.github.gomboksu.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import mci.github.gomboksu.R
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState
import mci.github.gomboksu.ui.rememberScrollConnection

data class Contributor(
    val login: String,
    val name: String? = null,
    val githubUrl: String,
    val role: String,
    val subtitle: String? = null,
    val donationUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContributorsScreen(navigator: DestinationsNavigator) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (bottomBarScrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    val contributors = listOf(
        Contributor(
            login = "MCI49312",
            githubUrl = "https://github.com/MCI49312",
            role = stringResource(R.string.contributor_gomboksu_dev),
            subtitle = stringResource(R.string.contributor_no_donations),
            donationUrl = null
        ),
        Contributor(
            login = "rifsxd",
            name = "Rifat Azad",
            githubUrl = "https://github.com/rifsxd",
            role = stringResource(R.string.contributor_ksun_author),
            donationUrl = "https://github.com/KernelSU-Next/KernelSU-Next/tree/dev?tab=readme-ov-file#-donations"
        ),
        Contributor(
            login = "tiann",
            name = "Weishu",
            githubUrl = "https://github.com/tiann",
            role = stringResource(R.string.contributor_kernel_author),
            donationUrl = "https://www.patreon.com/weishu"
        ),
        Contributor(
            login = "fatalcoder524",
            githubUrl = "https://github.com/fatalcoder524",
            role = stringResource(R.string.contributor_frontend_maintainer),
            donationUrl = "https://github.com/sponsors/fatalcoder524"
        ),
        Contributor(
            login = "pershoot",
            githubUrl = "https://github.com/pershoot",
            role = stringResource(R.string.contributor_backend_maintainer),
            donationUrl = "https://github.com/sponsors/pershoot"
        ),
        Contributor(
            login = "maxsteeel",
            name = "Max",
            githubUrl = "https://github.com/maxsteeel",
            role = stringResource(R.string.contributor_legacy_maintainer),
            donationUrl = "https://github.com/sponsors/maxsteeel"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.contributors),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .let { modifier ->
                    if (bottomBarScrollConnection != null) {
                        modifier
                            .nestedScroll(bottomBarScrollConnection)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
                .padding(bottom = navBarPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    contributors.forEach { contributor ->
                        ContributorRow(
                            contributor = contributor,
                            onProfileClick = { uriHandler.openUri(contributor.githubUrl) },
                            onDonateClick = contributor.donationUrl?.let { url ->
                                { uriHandler.openUri(url) }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier)
        }
    }
}

@Composable
private fun ContributorRow(
    contributor: Contributor,
    onProfileClick: () -> Unit,
    onDonateClick: (() -> Unit)? = null
) {
    var imageLoadFailed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar + name/role
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onProfileClick() }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!imageLoadFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://avatars.githubusercontent.com/${contributor.login}?s=80")
                            .crossfade(true)
                            .build(),
                        contentDescription = contributor.login,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { imageLoadFailed = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Name + role
            Column {
                Text(
                    text = contributor.name?.takeIf { it.isNotBlank() }
                        ?.let { "${contributor.login} ($it)" }
                        ?: contributor.login,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                contributor.subtitle?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (onDonateClick != null) {
            OutlinedButton(
                onClick = onDonateClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.support),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
