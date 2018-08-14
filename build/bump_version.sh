#!/bin/bash
#
# Copyright 2018 Deere & Company
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

VERSION=$1
CURRENT_VERSION=$(grep -o "\([0-9\.]*-SNAPSHOT\)" pom.xml)

REGEX="([0-9]+)\.([0-9]+)\.([0-9]+)(-SNAPSHOT)?"

echo ${CURRENT_VERSION}
if [[ $CURRENT_VERSION =~ $REGEX ]]
then
    MAJOR=${BASH_REMATCH[1]}
    MINOR=${BASH_REMATCH[2]}
    PATCH=${BASH_REMATCH[3]}
else
    echo "Err! Could not find version!"
    exit 1
fi


if [[ ${VERSION} == "MAJOR" ]]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
elif [[ ${VERSION} == "MINOR" ]]; then
    MINOR=$((MINOR + 1))
    PATCH=0
else
    PATCH=$((PATCH + 1))
fi

NEW_VERSION="$MAJOR.$MINOR.$PATCH-SNAPSHOT"
echo "Bumping to $VERSION version"
echo "Current version = $CURRENT_VERSION"
echo "New version     = $NEW_VERSION"
mvn --batch-mode release:update-versions -DdevelopmentVersion=${NEW_VERSION} -DautoVersionSubmodules=true
