package de.hsrm.mi.enia.moodplayer.presentation.views;

import de.hsrm.mi.enia.moodplayer.business.Track;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * eigene ListCell für die Playlist.
 *
 * eeigt:
 * - Titel
 * - Artist
 * - Album
 *
 * wird von der ListView gecached.
 */

public class TrackCell extends ListCell<Track> {
	HBox content;
	
	Label title;
	Label artist;
	Label album;
	
	public TrackCell() {
		content = new HBox();
		
		VBox trackInfo = new VBox();
		
		title = new Label();
		artist = new Label();
		album = new Label();
		
		title.getStyleClass().add("text-1");
		artist.getStyleClass().add("text-2");
		album.getStyleClass().add("text-2");
		
		trackInfo.getChildren().addAll(title, artist, album);
		trackInfo.setSpacing(5);
		
		content.getChildren().add(trackInfo);
		content.setAlignment(Pos.CENTER_LEFT);
		content.setSpacing(10);
		content.setPadding(new Insets(8, 15, 8, 15));
		
		this.setGraphic(content);
	}
	
	public TrackCell(int nr) {
		this();
		// Nummerierung nicht mehr verwendet
	}
	
	@Override
	public void updateItem(Track item, boolean empty) {
		super.updateItem(item, empty);
		
		if (!empty) {
			title.setText(item.getTitle());
			artist.setText(item.getArtist());
			album.setText(item.getAlbum());
			setGraphic(content);
		} else {
			setGraphic(null);
		}
	}

}