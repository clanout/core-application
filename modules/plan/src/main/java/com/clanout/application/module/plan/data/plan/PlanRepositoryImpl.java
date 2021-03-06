package com.clanout.application.module.plan.data.plan;

import com.clanout.application.library.mongo.MongoDataSource;
import com.clanout.application.library.mongo.MongoDateTimeMapper;
import com.clanout.application.library.postgres.PostgresDataSource;
import com.clanout.application.library.postgres.PostgresQuery;
import com.clanout.application.library.util.common.StringUtils;
import com.clanout.application.module.plan.domain.exception.PlanNotFoundException;
import com.clanout.application.module.plan.domain.model.Attendee;
import com.clanout.application.module.plan.domain.model.Location;
import com.clanout.application.module.plan.domain.model.Plan;
import com.clanout.application.module.plan.domain.model.PlanSuggestion;
import com.clanout.application.module.plan.domain.repository.PlanRepository;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Consumer;

public class PlanRepositoryImpl implements PlanRepository
{
    private static Logger LOG = LogManager.getRootLogger();

    private static final String MONGO_PLAN_COLLECTION = "plans";
    private static final String MONGO_PLAN_ARCHIVE_COLLECTION = "plans_archive";

    private static final String SQL_READ_ATTENDEE_NAME = PostgresQuery.load("read_attendee_name.sql", PlanRepositoryImpl.class);
    private static final String SQL_INSERT_PHONE_INVITATIONS = PostgresQuery.load("insert_phone_invitations.sql", PlanRepositoryImpl.class);
    private static final String SQL_READ_PHONE_INVITATIONS = PostgresQuery.load("read_phone_invitations.sql", PlanRepositoryImpl.class);
    private static final String SQL_DELETE_PHONE_INVITATIONS = PostgresQuery.load("delete_phone_invitations.sql", PlanRepositoryImpl.class);
    private static final String SQL_READ_PLAN_SUGGESTIONS = PostgresQuery.load("read_plan_suggestions.sql", PlanRepositoryImpl.class);

    @Override
    public Plan fetch(String planId) throws PlanNotFoundException
    {
        Document planDocument = null;
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> planCollection = database.getCollection(MONGO_PLAN_COLLECTION);
            planDocument = planCollection.find(new Document("_id", new ObjectId(planId))).first();
            if (planDocument == null)
            {
                throw new NullPointerException();
            }

            return MongoPlanMapper.map(planDocument, null);
        }
        catch (Exception e)
        {
            throw new PlanNotFoundException();
        }
    }

    @Override
    public Plan create(Plan plan)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_COLLECTION);

            Document document = new Document();

            document.put("title", plan.getTitle());
            document.put("type", plan.getType().name());
            document.put("category", plan.getCategory());
            document.put("creator_id", plan.getCreatorId());

            document.put("visibility_zones", plan.getVisibilityZones());

            if (plan.getDescription() != null)
            {
                document.put("description", plan.getDescription());
            }

            document.put("start_time", MongoDateTimeMapper.map(plan.getStartTime()));
            document.put("end_time", MongoDateTimeMapper.map(plan.getEndTime()));

            Location location = plan.getLocation();
            if (location != null)
            {
                Document locationDoc = new Document();
                locationDoc.put("name", location.getName());
                locationDoc.put("latitude", location.getLatitude());
                locationDoc.put("longitude", location.getLongitude());

                document.put("location", locationDoc);
            }

            document.put("created_at", MongoDateTimeMapper.map(plan.getCreatedAt()));
            document.put("updated_at", MongoDateTimeMapper.map(plan.getUpdatedAt()));

            List<Attendee> attendees = plan.getAttendees();
            ArrayList<Document> attendeeDocuments = new ArrayList<>();
            for (Attendee attendee : attendees)
            {
                Document attendeeDocument = new Document();
                attendeeDocument.put("id", attendee.getId());
                attendeeDocument.put("name", attendee.getName());
                attendeeDocument.put("status", attendee.getStatus());
                attendeeDocuments.add(attendeeDocument);
            }
            document.put("attendees", attendeeDocuments);

            collection.insertOne(document);
            plan.setId(document.getObjectId("_id").toHexString());

            return plan;
        }
        catch (Exception e)
        {
            LOG.error("Unable to create plan [" + e.getMessage() + "]");
            return null;
        }
    }

    @Override
    public void delete(String planId)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_COLLECTION);
            collection.findOneAndDelete(new Document("_id", new ObjectId(planId)));
        }
        catch (Exception e)
        {
            LOG.error("Unable to remove plan [" + e.getMessage() + "]");
        }
    }

    @Override
    public void update(String planId, String description, OffsetDateTime startTime,
                       OffsetDateTime endTime, Location location)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_COLLECTION);

            boolean isUpdated = false;
            Document updateObject = new Document();
            if (description != null)
            {
                isUpdated = true;
                updateObject.put("description", description);
            }

            if (startTime != null)
            {
                isUpdated = true;
                updateObject.put("start_time", MongoDateTimeMapper.map(startTime));
            }

            if (endTime != null)
            {
                isUpdated = true;
                updateObject.put("end_time", MongoDateTimeMapper.map(endTime));
            }

            if (location != null)
            {
                isUpdated = true;

                if (!StringUtils.isNullOrEmpty(location.getName()))
                {
                    Document locationDocument = new Document();
                    locationDocument.put("name", location.getName());
                    locationDocument.put("latitude", location.getLatitude());
                    locationDocument.put("longitude", location.getLongitude());
                    updateObject.put("location", locationDocument);
                }
                else
                {
                    updateObject.put("location", null);
                }
            }

            if (isUpdated)
            {
                updateObject.put("updated_at", MongoDateTimeMapper.map(OffsetDateTime.now(ZoneOffset.UTC)));
                collection.updateOne(new Document("_id", new ObjectId(planId)), new Document("$set", updateObject));
            }
        }
        catch (Exception e)
        {
            LOG.error("Unable to remove plan [" + e.getMessage() + "]");
        }
    }

    @Override
    public String getAttendeeName(String userId)
    {
        try (Connection connection = PostgresDataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_READ_ATTENDEE_NAME))
        {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next())
            {
                String name = resultSet.getString("attendee_name");
                resultSet.close();
                return name;
            }
            else
            {
                resultSet.close();
                return null;
            }
        }
        catch (SQLException e)
        {
            LOG.error("Unable to read attendee name [" + e.getSQLState() + " : " + e.getMessage() + "]");
            throw new RuntimeException();
        }
    }

    @Override
    public void addAttendee(String planId, Attendee attendee)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_COLLECTION);

            UpdateOptions updateOptions = new UpdateOptions();
            updateOptions.upsert(true);

            Document attendeeEntry = new Document();
            attendeeEntry.put("id", attendee.getId());
            attendeeEntry.put("name", attendee.getName());
            attendeeEntry.put("status", attendee.getStatus());

            /* Remove Plan Entry (if exists) */
            Document updateObject = new Document();
            updateObject.put("$pull", new Document("attendees", new Document("id", attendee.getId())));
            collection.updateOne(new BasicDBObject("_id", new ObjectId(planId)), updateObject, updateOptions);

            /* Insert Plan Entry */
            updateObject = new Document();
            updateObject.put("$addToSet", new Document("attendees", attendeeEntry));
            updateObject.put("$set", new Document("updated_at", MongoDateTimeMapper.map(OffsetDateTime.now(ZoneOffset.UTC))));
            collection.updateOne(new Document("_id", new ObjectId(planId)), updateObject, updateOptions);
        }
        catch (Exception e)
        {
            LOG.error("Unable to add attendee to plan [" + e.getMessage() + "]");
        }
    }

    @Override
    public void deleteAttendee(String planId, String userId)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_COLLECTION);

            Document updateObject = new Document();
            updateObject.put("$pull", new Document("attendees", new Document("id", userId)));
            updateObject.put("$set", new Document("updated_at", MongoDateTimeMapper.map(OffsetDateTime.now(ZoneOffset.UTC))));
            collection.updateOne(new BasicDBObject("_id", new ObjectId(planId)), updateObject);
        }
        catch (Exception e)
        {
            LOG.error("Unable to remove attendee from plan [" + e.getMessage() + "]");
        }
    }

    @Override
    public void updateStatus(String planId, String userId, String status)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> planCollection = database.getCollection(MONGO_PLAN_COLLECTION);

            Document searchQuery = new Document();
            searchQuery.put("_id", new ObjectId(planId));
            searchQuery.put("attendees.id", userId);

            Document updateObject = new Document();
            updateObject.put("attendees.$.status", status);
            updateObject.put("updated_at", MongoDateTimeMapper.map(OffsetDateTime.now(ZoneOffset.UTC)));
            planCollection.updateOne(searchQuery, new Document("$set", updateObject));
        }
        catch (Exception e)
        {
            LOG.error("Unable to update status in plan attendee list [" + e.getMessage() + "]");
            throw new RuntimeException();
        }
    }

    @Override
    public void addPhoneInvitations(String planId, String userId, List<String> mobileNumbers)
    {
        try (Connection connection = PostgresDataSource.getInstance().getConnection())
        {
            for (String mobileNumber : mobileNumbers)
            {
                try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT_PHONE_INVITATIONS))
                {
                    statement.setString(1, mobileNumber);
                    statement.setString(2, planId);
                    statement.setString(3, userId);
                    statement.executeUpdate();
                }
                catch (SQLException e)
                {
                    LOG.error("Unable to create phone invitation [" + e.getSQLState() + " : " + e.getMessage() + "]");
                }
            }
        }
        catch (SQLException e)
        {
        }
    }

    @Override
    public Map<String, Set<String>> fetchPendingInvitations(String mobileNumber)
    {
        try (Connection connection = PostgresDataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_READ_PHONE_INVITATIONS))
        {
            statement.setString(1, mobileNumber);

            Map<String, Set<String>> result = new HashMap<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next())
            {
                String userId = resultSet.getString("user_id");
                String planId = resultSet.getString("plan_id");

                if (result.containsKey(planId))
                {
                    result.get(planId).add(userId);
                }
                else
                {
                    Set<String> userIds = new HashSet<>();
                    userIds.add(userId);
                    result.put(planId, userIds);
                }
            }
            resultSet.close();

            return result;
        }
        catch (SQLException e)
        {
            LOG.error("Unable to read phone invitation [" + e.getSQLState() + " : " + e.getMessage() + "]");
            throw new RuntimeException();
        }
    }

    @Override
    public void deletePendingInvitations(String mobileNumber)
    {
        try (Connection connection = PostgresDataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_DELETE_PHONE_INVITATIONS))
        {
            statement.setString(1, mobileNumber);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            LOG.error("Unable to delete phone invitations [" + e.getSQLState() + " : " + e.getMessage() + "]");
        }
    }

    @Override
    public List<PlanSuggestion> fetchCreateSuggestions()
    {
        try (Connection connection = PostgresDataSource.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_READ_PLAN_SUGGESTIONS))
        {
            List<PlanSuggestion> suggestions = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next())
            {
                String category = resultSet.getString("category");
                String title = resultSet.getString("title");
                suggestions.add(new PlanSuggestion(category, title));
            }
            resultSet.close();

            return suggestions;
        }
        catch (SQLException e)
        {
            LOG.error("Unable to read create plan suggestions [" + e.getSQLState() + " : " + e.getMessage() + "]");
            throw new RuntimeException();
        }
    }

    @Override
    public List<Plan> fetchExpiredPlans(OffsetDateTime timestamp)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> planCollection = database.getCollection(MONGO_PLAN_COLLECTION);

            List<Plan> plans = new ArrayList<>();
            Document query = new Document();
            query.put("end_time", new Document("$lt", MongoDateTimeMapper.map(timestamp)));
            planCollection
                    .find(query)
                    .forEach((Consumer<Document>) document -> {
                        try
                        {
                            plans.add(MongoPlanMapper.map(document, null));
                        }
                        catch (Exception e)
                        {
                            LOG.error("Unable to read plan [" + e.getMessage() + "]");
                        }
                    });

            return plans;
        }
        catch (Exception e)
        {
            LOG.error("Unable to read expired plans [" + e.getMessage() + "]");
            throw new RuntimeException();
        }
    }

    @Override
    public void archive(Plan plan)
    {
        try
        {
            MongoDatabase database = MongoDataSource.getInstance().getDatabase();
            MongoCollection<Document> collection = database.getCollection(MONGO_PLAN_ARCHIVE_COLLECTION);

            Document document = new Document();

            document.put("_id", new ObjectId(plan.getId()));
            document.put("title", plan.getTitle());
            document.put("type", plan.getType().name());
            document.put("category", plan.getCategory());
            document.put("creator_id", plan.getCreatorId());

            document.put("visibility_zones", plan.getVisibilityZones());

            if (plan.getDescription() != null)
            {
                document.put("description", plan.getDescription());
            }

            document.put("start_time", MongoDateTimeMapper.map(plan.getStartTime()));
            document.put("end_time", MongoDateTimeMapper.map(plan.getEndTime()));

            Location location = plan.getLocation();
            if (location != null)
            {
                Document locationDoc = new Document();
                locationDoc.put("name", location.getName());
                locationDoc.put("latitude", location.getLatitude());
                locationDoc.put("longitude", location.getLongitude());

                document.put("location", locationDoc);
            }

            document.put("created_at", MongoDateTimeMapper.map(plan.getCreatedAt()));
            document.put("updated_at", MongoDateTimeMapper.map(plan.getUpdatedAt()));

            List<Attendee> attendees = plan.getAttendees();
            ArrayList<Document> attendeeDocuments = new ArrayList<>();
            for (Attendee attendee : attendees)
            {
                Document attendeeDocument = new Document();
                attendeeDocument.put("id", attendee.getId());
                attendeeDocument.put("name", attendee.getName());
                attendeeDocument.put("status", attendee.getStatus());
                attendeeDocuments.add(attendeeDocument);
            }
            document.put("attendees", attendeeDocuments);

            collection.insertOne(document);


        }
        catch (Exception e)
        {
            LOG.error("Unable to archive plan [" + e.getMessage() + "]");
        }
    }
}
