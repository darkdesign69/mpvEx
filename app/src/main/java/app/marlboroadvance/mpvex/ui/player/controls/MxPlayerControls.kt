package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.Decoder
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.VideoAspect
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekbarWithTimers
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

/** Buttons that already exist somewhere else in the MX layout (top bar / lock button). */
private val MX_EXCLUDED_BUTTONS =
  setOf(
    PlayerButton.BACK_ARROW,
    PlayerButton.VIDEO_TITLE,
    PlayerButton.LOCK_CONTROLS,
    PlayerButton.MORE_OPTIONS,
    PlayerButton.NONE,
  )

/**
 * MX Player style controls for portrait mode:
 *  - black top bar: back, title, decoder badge, more menu
 *  - round shortcut buttons over the top of the video
 *  - black bottom bar: seek bar with times, then lock + previous / play / next
 */
@Composable
fun MxPortraitControls(
  viewModel: PlayerViewModel,
  activity: PlayerActivity,
  mediaTitle: String?,
  buttons: List<PlayerButton>,
  chapters: List<Segment>,
  currentChapter: Int?,
  isSpeedNonOne: Boolean,
  currentZoom: Float,
  aspect: VideoAspect,
  decoder: Decoder,
  playbackSpeed: Float,
  paused: Boolean,
  position: Float,
  duration: Float,
  loopStart: Float?,
  loopEnd: Float?,
  onBackPress: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  onSeekingChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val playerPreferences = koinInject<PlayerPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val invertDuration by playerPreferences.invertDuration.collectAsState()
  val seekbarStyle by appearancePreferences.seekbarStyle.collectAsState()

  val shortcutButtons = remember(buttons) { buttons.filter { it !in MX_EXCLUDED_BUTTONS } }
  var seeking by remember { mutableStateOf(false) }
  var wasPausedBeforeSeek by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    // ---------------- Top bar ----------------
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Color.Black)
          .windowInsetsPadding(WindowInsets.statusBars),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBackPress) {
          Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
          )
        }
        Text(
          text = mediaTitle ?: "",
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier =
            Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
              .clickable(enabled = viewModel.hasPlaylistSupport()) {
                clickEvent()
                onOpenSheet(Sheets.Playlist)
              },
        )
        Text(
          text = decoder.title,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          modifier =
            Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable {
                clickEvent()
                onOpenSheet(Sheets.Decoders)
              }.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        IconButton(
          onClick = {
            clickEvent()
            onOpenSheet(Sheets.More)
          },
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More",
            tint = Color.White,
          )
        }
      }
    }

    // ---------------- Round shortcut buttons over the video ----------------
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      shortcutButtons.forEach { button ->
        RenderPlayerButton(
          button = button,
          chapters = chapters,
          currentChapter = currentChapter,
          isPortrait = true,
          isSpeedNonOne = isSpeedNonOne,
          currentZoom = currentZoom,
          aspect = aspect,
          mediaTitle = mediaTitle,
          hideBackground = false,
          decoder = decoder,
          playbackSpeed = playbackSpeed,
          onBackPress = onBackPress,
          onOpenSheet = onOpenSheet,
          onOpenPanel = onOpenPanel,
          viewModel = viewModel,
          activity = activity,
          buttonSize = 44.dp,
        )
      }
    }

    // Empty middle: touches fall through to the gesture layer
    Spacer(modifier = Modifier.weight(1f))

    // ---------------- Bottom bar ----------------
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Color.Black)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
      SeekbarWithTimers(
        position = position,
        duration = duration,
        onValueChange = {
          if (!seeking) {
            wasPausedBeforeSeek = paused
            if (!paused) viewModel.pause()
            seeking = true
            onSeekingChanged(true)
          }
          clickEvent()
          viewModel.seekTo(it.toInt())
        },
        onValueChangeFinished = {
          seeking = false
          onSeekingChanged(false)
          clickEvent()
          if (!wasPausedBeforeSeek) viewModel.unpause()
          viewModel.showControls()
        },
        timersInverted = Pair(false, invertDuration),
        durationTimerOnCLick = {
          clickEvent()
          playerPreferences.invertDuration.set(!invertDuration)
        },
        positionTimerOnClick = {},
        chapters = chapters.toImmutableList(),
        paused = paused,
        seekbarStyle = seekbarStyle,
        loopStart = loopStart,
        loopEnd = loopEnd,
      )

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
          IconButton(onClick = viewModel::lockControls) {
            Icon(
              imageVector = Icons.Default.LockOpen,
              contentDescription = "Lock controls",
              tint = Color.White,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val hasPrevious = viewModel.hasPrevious()
          val hasNext = viewModel.hasNext()

          IconButton(
            onClick = {
              clickEvent()
              if (viewModel.hasPrevious()) viewModel.playPrevious()
            },
            enabled = hasPrevious,
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous",
              tint = Color.White.copy(alpha = if (hasPrevious) 1f else 0.38f),
              modifier = Modifier.size(36.dp),
            )
          }

          IconButton(
            onClick = {
              clickEvent()
              viewModel.pauseUnpause()
            },
            modifier = Modifier.size(64.dp),
          ) {
            Icon(
              imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = if (paused) "Play" else "Pause",
              tint = Color.White,
              modifier = Modifier.size(52.dp),
            )
          }

          IconButton(
            onClick = {
              clickEvent()
              if (viewModel.hasNext()) viewModel.playNext()
            },
            enabled = hasNext,
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next",
              tint = Color.White.copy(alpha = if (hasNext) 1f else 0.38f),
              modifier = Modifier.size(36.dp),
            )
          }
        }

        Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}
