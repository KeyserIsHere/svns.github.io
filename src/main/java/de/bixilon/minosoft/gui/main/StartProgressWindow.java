/*
 * Codename Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.main;

import de.bixilon.minosoft.data.locale.LocaleManager;
import de.bixilon.minosoft.data.locale.Strings;
import de.bixilon.minosoft.logging.Log;
import de.bixilon.minosoft.util.CountUpAndDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class StartProgressWindow extends Application {
    public static CountDownLatch toolkitLatch = new CountDownLatch(2);

    public static void show(CountUpAndDownLatch progress) {
        new Thread(() -> {
            if (progress.getCount() == 0) {
                return;
            }
            AtomicReference<ProgressBar> progressBar = new AtomicReference<>();
            AtomicReference<Dialog<Boolean>> progressDialog = new AtomicReference<>();
            Platform.runLater(() -> {
                progressDialog.set(new Dialog<>());
                progressDialog.get().setTitle(LocaleManager.translate(Strings.MINOSOFT_STILL_STARTING_TITLE));
                progressDialog.get().setHeaderText(LocaleManager.translate(Strings.MINOSOFT_STILL_STARTING_HEADER));
                GridPane grid = new GridPane();
                progressBar.set(new ProgressBar());
                progressBar.get().setProgress(1.0F - ((float) progress.getCount() / progress.getTotal()));
                grid.add(progressBar.get(), 0, 0);
                progressDialog.get().getDialogPane().setContent(grid);
                progressDialog.get().show();

                Stage stage = (Stage) progressDialog.get().getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
                stage.toFront();

            });
            while (progress.getCount() > 0) {
                try {
                    progress.waitForChange();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> progressBar.get().setProgress(1.0F - ((float) progress.getCount() / progress.getTotal())));
            }

            Platform.runLater(() -> {
                progressDialog.get().setResult(Boolean.TRUE);
                progressDialog.get().hide();
            });
        }).start();
    }

    public static void start() throws InterruptedException {
        Log.debug("Initializing JavaFX Toolkit...");
        toolkitLatch.countDown();
        new Thread(Application::launch).start();
        toolkitLatch.await();
        Log.debug("Initialized JavaFX Toolkit!");
    }

    @Override
    public void start(Stage stage) {
        toolkitLatch.countDown();
    }
}