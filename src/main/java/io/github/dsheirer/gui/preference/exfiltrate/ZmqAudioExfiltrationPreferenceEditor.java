/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.preference.exfiltrate;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.exfiltrate.ZmqAudioExfiltrationPreference;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preference settings for ZeroMQ audio exfiltration
 */
public class ZmqAudioExfiltrationPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(ZmqAudioExfiltrationPreferenceEditor.class);
    private ZmqAudioExfiltrationPreference mPreference;
    private GridPane mEditorPane;
    private ToggleSwitch mEnabledToggleSwitch;
    private TextField mEndpointTextField;

    /**
     * Constructs an instance
     */
    public ZmqAudioExfiltrationPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getZmqAudioExfiltrationPreference();

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
            mEditorPane.setHgap(10);
            mEditorPane.setVgap(10);

            int row = 0;

            // Title label
            Label titleLabel = new Label("ZeroMQ Audio Exfiltration");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            GridPane.setHalignment(titleLabel, HPos.CENTER);
            mEditorPane.add(titleLabel, 0, row, 3, 1);

            // Description
            Label descriptionLabel = new Label("Real-time streaming of decoded DMR audio with metadata via ZeroMQ");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setStyle("-fx-text-fill: gray;");
            GridPane.setHalignment(descriptionLabel, HPos.CENTER);
            mEditorPane.add(descriptionLabel, 0, ++row, 3, 1);

            // Separator
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, ++row, 3, 1);

            // Enable/Disable toggle
            mEditorPane.add(getEnabledToggleSwitch(), 0, ++row);
            Label enabledLabel = new Label("Enable ZMQ Audio Exfiltration");
            enabledLabel.setWrapText(true);
            mEditorPane.add(enabledLabel, 1, row, 2, 1);

            // Endpoint configuration
            Label endpointLabel = new Label("ZeroMQ Endpoint:");
            GridPane.setHalignment(endpointLabel, HPos.RIGHT);
            mEditorPane.add(endpointLabel, 0, ++row);
            mEditorPane.add(getEndpointTextField(), 1, row, 2, 1);

            // Help text
            Label helpLabel = new Label("Format: tcp://*:port (e.g., tcp://*:15023 for all interfaces, tcp://127.0.0.1:15023 for localhost only)");
            helpLabel.setWrapText(true);
            helpLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
            mEditorPane.add(helpLabel, 1, ++row, 2, 1);

            // Information section
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, ++row, 3, 1);

            Label infoTitleLabel = new Label("Information");
            infoTitleLabel.setStyle("-fx-font-weight: bold;");
            mEditorPane.add(infoTitleLabel, 0, ++row, 3, 1);

            Label infoLabel = new Label(
                "• Publishes JSON messages with PCM 16-bit audio and metadata\n" +
                "• Includes FROM/TO IDs with aliases, frequency, timeslot, and LCN\n" +
                "• Uses ZeroMQ PUB/SUB pattern for efficient distribution\n" +
                "• Can be consumed by Python scripts or other ZMQ clients\n" +
                "• See ZMQ_AUDIO_EXFILTRATION.md for details and examples"
            );
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
            mEditorPane.add(infoLabel, 0, ++row, 3, 1);
        }

        return mEditorPane;
    }

    private ToggleSwitch getEnabledToggleSwitch()
    {
        if(mEnabledToggleSwitch == null)
        {
            mEnabledToggleSwitch = new ToggleSwitch();
            mEnabledToggleSwitch.setSelected(mPreference.isEnabled());
            mEnabledToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                mPreference.setEnabled(newValue);
                getEndpointTextField().setDisable(!newValue);
            });
        }

        return mEnabledToggleSwitch;
    }

    private TextField getEndpointTextField()
    {
        if(mEndpointTextField == null)
        {
            mEndpointTextField = new TextField();
            mEndpointTextField.setText(mPreference.getEndpoint());
            mEndpointTextField.setDisable(!mPreference.isEnabled());
            mEndpointTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null && !newValue.trim().isEmpty())
                {
                    mPreference.setEndpoint(newValue.trim());
                }
            });
        }

        return mEndpointTextField;
    }
}
