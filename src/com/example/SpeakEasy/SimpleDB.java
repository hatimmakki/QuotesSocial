/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.example.SpeakEasy;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleDB {

    private static String nextToken = null;
    private static int prevNumDomains = 0;
    public static final String QUOTES = "Quotes";

    public static AmazonSimpleDBClient getInstance() {
        if (MainPage.clientManager != null)
            return MainPage.clientManager.sdb();
        else return HomePage.clientManager.sdb();
    }

    public static List<String> getDomainNames() {
        return getInstance().listDomains().getDomainNames();
    }

    public static List<String> getDomainNames(int numDomains) {
        prevNumDomains = numDomains;
        return getDomainNames(numDomains, null);
    }

    private static List<String> getDomainNames(int numDomains, String nextToken) {
        ListDomainsRequest req = new ListDomainsRequest();
        req.setMaxNumberOfDomains(numDomains);
        if (nextToken != null)
            req.setNextToken(nextToken);
        ListDomainsResult result = getInstance().listDomains(req);
        List<String> domains = result.getDomainNames();
        SimpleDB.nextToken = result.getNextToken();
        return domains;
    }

    public static List<String> getMoreDomainNames() {
        if (nextToken == null) {
            return new ArrayList<String>();
        } else {
            return getDomainNames(prevNumDomains, nextToken);
        }

    }

    public static void createDomain(String domainName) {
        getInstance().createDomain(new CreateDomainRequest(domainName));
    }

    public static void deleteDomain(String domainName) {
        getInstance().deleteDomain(new DeleteDomainRequest(domainName));
    }

    public static void createItem(String domainName, String itemName) {
        List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>(1);
        attributes.add(new ReplaceableAttribute().withName("Name").withValue("Value"));
        getInstance().putAttributes(new PutAttributesRequest(domainName, itemName, attributes));
    }

    public static void createAttributeForItem(String domainName, String itemName, String attributeName, String attributeValue) {
        List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>(1);
        attributes.add(new ReplaceableAttribute().withName(attributeName).withValue(attributeValue).withReplace(true));
        getInstance().putAttributes(new PutAttributesRequest(domainName, itemName, attributes));
    }

    public static String[] getItemNamesForDomain(String domainName) {
        SelectRequest selectRequest = new SelectRequest("select itemName() from `" + domainName + "`").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();

        String[] itemNames = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemNames[i] = ((Item) items.get(i)).getName();
        }

        return itemNames;
    }

    public static HashMap<String, String> getAttributesForItem(String domainName, String itemName) {
        GetAttributesRequest getRequest = new GetAttributesRequest(domainName, itemName).withConsistentRead(true);
        GetAttributesResult getResult = getInstance().getAttributes(getRequest);

        HashMap<String, String> attributes = new HashMap<String, String>(30);
        for (Object attribute : getResult.getAttributes()) {
            String name = ((Attribute) attribute).getName();
            String value = ((Attribute) attribute).getValue();

            attributes.put(name, value);
        }

        return attributes;
    }

    public static void updateAttributesForItem(String domainName, String itemName, HashMap<String, String> attributes) {
        List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>(attributes.size());

        for (String attributeName : attributes.keySet()) {
            replaceableAttributes.add(new ReplaceableAttribute().withName(attributeName).withValue(attributes.get(attributeName)).withReplace(true));
        }

        getInstance().putAttributes(new PutAttributesRequest(domainName, itemName, replaceableAttributes));
    }

    public static void deleteItem(String domainName, String itemName) {
        getInstance().deleteAttributes(new DeleteAttributesRequest(domainName, itemName));
    }

    public static void deleteItemAttribute(String domainName, String itemName, String attributeName) {
        getInstance().deleteAttributes(new DeleteAttributesRequest(domainName, itemName).withAttributes(new Attribute[]{new Attribute().withName(attributeName)}));
    }

    public static void addToFavoriteTable(String postID, String accountName) {
        ReplaceableAttribute favoritedPostID = new ReplaceableAttribute("postID", postID, Boolean.FALSE);
        ReplaceableAttribute accName = new ReplaceableAttribute("likedBy", accountName, Boolean.FALSE);

        List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>(2);
        attrs.add(favoritedPostID);
        attrs.add(accName);


        PutAttributesRequest par = new PutAttributesRequest("Favorites", postID + "_likedBy_" + accountName, attrs);
        try {
            getInstance().putAttributes(par);
        } catch (Exception exception) {
            System.out.println("EXCEPTION = " + exception);
        }
    }

    public static void addPostIDToDomain(String postID, String tableName) {
        ReplaceableAttribute postIDAttribute = new ReplaceableAttribute("postID", postID, Boolean.FALSE);
        List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>(1);
        attrs.add(postIDAttribute);

        PutAttributesRequest par = new PutAttributesRequest(tableName, postID, attrs);
        try {
            getInstance().putAttributes(par);
        } catch (Exception exception) {
            System.out.println("EXCEPTION = " + exception);
        }
    }

    public static void addQuote(QuotePost quote) {

        ReplaceableAttribute quoteAttribute = new ReplaceableAttribute("quoteText", quote.getQuoteText(), Boolean.FALSE);
        ReplaceableAttribute authorAttribute = new ReplaceableAttribute("author", quote.getAuthorName(), Boolean.FALSE);
        ReplaceableAttribute timeAttribute = new ReplaceableAttribute("timestamp", "" + quote.getTimestamp(), Boolean.FALSE);
        ReplaceableAttribute fbNameAttribute = new ReplaceableAttribute("fbName", quote.getFbName(), Boolean.FALSE);
        ReplaceableAttribute numFavorites = new ReplaceableAttribute("favorites", "0", Boolean.TRUE);

        ArrayList<Integer> categories = quote.getCategories();
        int numCategories = categories.size();

        List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>(5 + numCategories);
        attrs.add(quoteAttribute);
        attrs.add(authorAttribute);
        attrs.add(timeAttribute);
        attrs.add(fbNameAttribute);
        attrs.add(numFavorites);

        for (int i = 0; i < numCategories; i++) {
            switch ((Integer) categories.get(i)) {
                case 0:
                    attrs.add(new ReplaceableAttribute("category", "advice", Boolean.FALSE));
                    break;
                case 1:
                    attrs.add(new ReplaceableAttribute("category", "funny", Boolean.FALSE));
                    break;
                case 2:
                    attrs.add(new ReplaceableAttribute("category", "inspirational", Boolean.FALSE));
                    break;
                case 3:
                    attrs.add(new ReplaceableAttribute("category", "love", Boolean.FALSE));
                    break;
                case 4:
                    attrs.add(new ReplaceableAttribute("category", "movie", Boolean.FALSE));
                    break;
            }
        }

        PutAttributesRequest par = new PutAttributesRequest(QUOTES, quote.getFbName().replace(" ", "") + quote.getTimestamp(), attrs);
        try {
            getInstance().putAttributes(par);
        } catch (Exception exception) {
            System.out.println("EXCEPTION = " + exception);
        }
    }

    public static List<String> getMyQuotesItemNames(String myName) {
        SelectRequest selectRequest = new SelectRequest("select itemName() from Quotes where fbName = '" + myName + "' and timestamp is not null order by timestamp desc").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();

        List<String> itemNames = new ArrayList<String>();
        for (int i = 0; i < items.size(); i++) {
            itemNames.add(((Item) items.get(i)).getName());
        }

        return itemNames;
    }

    public static int favCount(String postId) {
        SelectRequest selectRequest = new SelectRequest("select favorites from Quotes where itemName() = '" + postId + "'").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();
        return Integer.parseInt(items.get(0).getAttributes().get(0).getValue());
    }

    public static boolean isFavoritedByUser(String postId, String name) {
        SelectRequest selectRequest = new SelectRequest("select itemName() from Favorites where postID = '" + postId + "' and likedBy = '" + name + "'").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();
        return items.size() > 0;
    }

    public static List<String> getFeedItemNames(String myName) {
        SelectRequest selectRequest = new SelectRequest("select itemName() from Quotes where fbName != '" + myName + "' and timestamp is not null order by timestamp desc").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();

        List<String> itemNames = new ArrayList<String>();
        for (int i = 0; i < items.size(); i++) {
            itemNames.add(((Item) items.get(i)).getName());
        }

        return itemNames;
    }

    public static List<String> getFeedItemNamesByCategory(String category) {
        SelectRequest selectRequest = new SelectRequest("select itemName() from Quotes where category = '" + category + "' and timestamp is not null order by timestamp desc").withConsistentRead(true);
        List<Item> items = getInstance().select(selectRequest).getItems();

        List<String> itemNames = new ArrayList<String>();
        for (int i = 0; i < items.size(); i++) {
            itemNames.add(((Item) items.get(i)).getName());
        }

        return itemNames;
    }

}
