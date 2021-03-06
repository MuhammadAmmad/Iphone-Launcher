/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import com.google.common.collect.HashMultiset;

import android.util.Log;

import java.util.ArrayList;

/**
 * A promoter that limits the maximum number of shortcuts per source
 * (from non-web soruces), and then delegates promotion to another promoter.
 */
public class ShortcutLimitingPromoter extends PromoterWrapper {

    private static final String TAG = "QSB.ShortcutLimitingPromoter";
    private static final boolean DBG = false;

    private final int mMaxShortcutsPerWebSource;
    private final int mMaxShortcutsPerNonWebSource;

    /**
     * Creates a new ShortcutPromoter.
     *
     * @param nextPromoter The promoter to use when there are no more shortcuts.
     *        May be {@code null}.
     */
    public ShortcutLimitingPromoter(int maxShortcutsPerWebSource,
            int maxShortcutsPerNonWebSource, Promoter nextPromoter) {
        super(nextPromoter);
        mMaxShortcutsPerWebSource = maxShortcutsPerWebSource;
        mMaxShortcutsPerNonWebSource = maxShortcutsPerNonWebSource;
    }

    @Override
    public void pickPromoted(SuggestionCursor shortcuts,
            ArrayList<CorpusResult> suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        final int shortcutCount = shortcuts == null ? 0 : shortcuts.getCount();
        ListSuggestionCursor filteredShortcuts = null;
        if (shortcutCount > 0) {
            filteredShortcuts = new ListSuggestionCursor(shortcuts.getUserQuery());
            HashMultiset<Source> sourceShortcutCounts = HashMultiset.create(shortcutCount);
            int numPromoted = 0;
            for (int i = 0; i < shortcutCount; i++) {
                shortcuts.moveTo(i);
                Source source = shortcuts.getSuggestionSource();
                if (source != null) {
                    int prevCount = sourceShortcutCounts.add(source, 1);
                    if (DBG) Log.d(TAG, "Source: " + source + ", count: " + prevCount);
                    int maxShortcuts = source.isWebSuggestionSource()
                            ? mMaxShortcutsPerWebSource : mMaxShortcutsPerNonWebSource;
                    if (prevCount < maxShortcuts) {
                        numPromoted++;
                        filteredShortcuts.add(new SuggestionPosition(shortcuts));
                    }
                    if (numPromoted >= maxPromoted) break;
                }
            }
        }
        if (DBG) {
            Log.d(TAG, "pickPromoted shortcuts=" + shortcutCount + " filtered=" +
                    filteredShortcuts.getCount());
        }
        super.pickPromoted(filteredShortcuts, suggestions, maxPromoted, promoted);
    }

}
