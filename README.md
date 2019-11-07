# medtextanalyze
Sample AWS Lambda functions using Textract and Comprehend Medical to pull entities from images and PDFs

Intended to be used with  AWS Serverless Application Model (SAM) command line client. Ex:
> sam package --template-file template.yaml --s3-bucket <Your Code Bucket> --output-template-file packaged.yaml

Then deployed with Cloudformation command line client. Ex:
> aws cloudformation deploy --template-file packaged.yaml --stack-name medtextanalyze --capabilities CAPABILITY_IAM --region us-east-1
